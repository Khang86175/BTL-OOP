param(
 [int]$Port = 18083,
 [int]$DebugPort = 19223
)
$ErrorActionPreference='Stop'
$workspaceRoot=(Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$jar=Join-Path $workspaceRoot 'target/movie-0.0.1-SNAPSHOT.jar'
$edge='C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'
if(!(Test-Path -LiteralPath $edge)){throw 'Microsoft Edge not found.'}
if(!(Test-Path -LiteralPath $jar)){throw 'Build the JAR first.'}
$qaDir=Join-Path $workspaceRoot 'target/seat-designer-browser'
New-Item -ItemType Directory -Force -Path $qaDir | Out-Null
$base="http://127.0.0.1:$Port"
$app=$null
$browser=$null
$socket=$null
$script:sequence=0
$checks=New-Object 'System.Collections.Generic.List[string]'
function Assert-Check([bool]$ok,[string]$name){
 if(!$ok){throw "FAIL: $name"}
 $checks.Add($name)
 Write-Output "PASS: $name"
}
function Cdp([string]$method,[hashtable]$parameters=@{}){
 $script:sequence++
 $id=$script:sequence
 $data=@{id=$id;method=$method;params=$parameters}|ConvertTo-Json -Compress -Depth 60
 $bytes=[Text.Encoding]::UTF8.GetBytes($data)
 $timeout=New-Object Threading.CancellationTokenSource
 $timeout.CancelAfter(30000)
 try{
  $null=$socket.SendAsync([ArraySegment[byte]]::new($bytes),[Net.WebSockets.WebSocketMessageType]::Text,$true,$timeout.Token).GetAwaiter().GetResult()
  while($true){
   $stream=New-Object IO.MemoryStream
   try{
    do{
     $buffer=New-Object byte[] 65536
     $part=$socket.ReceiveAsync([ArraySegment[byte]]::new($buffer),$timeout.Token).GetAwaiter().GetResult()
     if($part.MessageType -eq [Net.WebSockets.WebSocketMessageType]::Close){throw 'Browser closed socket.'}
     $stream.Write($buffer,0,$part.Count)
    }while(!$part.EndOfMessage)
    $response=[Text.Encoding]::UTF8.GetString($stream.ToArray())|ConvertFrom-Json
   }finally{$stream.Dispose()}
   if($response.id -eq $id){
    if($response.error){throw ($response.error|ConvertTo-Json -Compress)}
    return $response.result
   }
  }
 }finally{$timeout.Dispose()}
}
function Js([string]$expression){
 $result=Cdp 'Runtime.evaluate' @{expression=$expression;returnByValue=$true;awaitPromise=$true}
 if($result.exceptionDetails){throw ($result.exceptionDetails|ConvertTo-Json -Compress -Depth 20)}
 return $result.result.value
}
function Wait-Js([string]$expression){
 for($attempt=0;$attempt -lt 120;$attempt++){
  try{if((Js $expression) -eq $true){return}}catch{}
  Start-Sleep -Milliseconds 250
 }
 throw "Timed out waiting for: $expression"
}
function Navigate([string]$path,[string]$ready){
 $null=Cdp 'Page.navigate' @{url="$base$path"}
 Wait-Js $ready
}
try{
 $app=Start-Process -FilePath java -ArgumentList @(
  '-jar',('"'+$jar+'"'),'--spring.profiles.active=demo','--showtime.seed.enabled=true',
  "--server.port=$Port",'--spring.datasource.url=jdbc:h2:mem:b3_browser;MODE=MySQL;DB_CLOSE_DELAY=-1',
  '--spring.datasource.driver-class-name=org.h2.Driver','--spring.datasource.username=sa','--spring.datasource.password='
 ) -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $qaDir 'app.log') -RedirectStandardError (Join-Path $qaDir 'app-error.log')
 $started=$false
 for($attempt=0;$attempt -lt 100;$attempt++){
  if($app.HasExited){throw 'Demo process exited; inspect target/seat-designer-browser/app.log.'}
  try{
   $r=Invoke-WebRequest "$base/login" -UseBasicParsing -TimeoutSec 2
   if($r.StatusCode -eq 200){$started=$true;break}
  }catch{}
  Start-Sleep -Milliseconds 500
 }
 if(!$started){throw 'H2 demo did not start.'}
 $browser=Start-Process -FilePath $edge -ArgumentList @(
  '--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check',
  "--remote-debugging-port=$DebugPort",'--remote-debugging-address=127.0.0.1',
  ('--user-data-dir="'+(Join-Path $qaDir "edge-profile-$PID")+'"'),'about:blank'
 ) -WindowStyle Hidden -PassThru
 $target=$null
 for($attempt=0;$attempt -lt 80;$attempt++){
  try{$targets=Invoke-RestMethod "http://127.0.0.1:$DebugPort/json/list";$target=$targets | Where-Object {$_.type -eq 'page'} | Select-Object -First 1;if($target){break}}catch{}
  Start-Sleep -Milliseconds 250
 }
 if(!$target){throw 'Edge debugging endpoint did not start.'}
 $socket=New-Object Net.WebSockets.ClientWebSocket
 $null=$socket.ConnectAsync([Uri]$target.webSocketDebuggerUrl,[Threading.CancellationToken]::None).GetAwaiter().GetResult()
 $null=Cdp 'Page.enable'
 $null=Cdp 'Emulation.setDeviceMetricsOverride' @{width=1600;height=1100;deviceScaleFactor=1;mobile=$false}
 Navigate '/login' "!!document.querySelector('input[name=username]')"
 $null=Js "document.querySelector('input[name=username]').value='admin@cineticket.test';document.querySelector('input[name=password]').value='DemoAdmin123!';document.querySelector('input[name=username]').form.requestSubmit();true"
 Wait-Js "!!document.querySelector('a[href=""/admin/rooms""]')"
 Navigate '/admin/rooms' "!!document.querySelector('a[href*=""/admin/seats?""]')"
 $demoLink=Js "document.querySelector('a[href*=""/admin/seats?""]').getAttribute('href')"
 Navigate $demoLink "!!document.getElementById('seat-designer') && typeof window.CineSeatDesigner==='object'"
 Assert-Check ((Js "document.querySelectorAll('#designer-seed [data-seat-id]').length===0 && document.querySelectorAll('.designer-row').length===0") -eq $true) 'Demo room starts empty without preset A/B/C'
 Navigate '/admin/rooms/new' "!!document.querySelector('select[name=theaterId]')"
 $null=Js "const t=document.querySelector('select[name=theaterId]');t.value=[...t.options].find(o=>o.value).value;document.querySelector('input[name=name]').value='QA Designer '+Date.now();document.querySelector('select[name=roomType]').value='TWO_D';document.querySelector('input[name=active]').checked=true;t.form.requestSubmit();true"
 Wait-Js "location.pathname==='/admin/rooms' && [...document.querySelectorAll('tr')].some(r=>r.textContent.includes('QA Designer'))"
 $roomLink=Js "[...document.querySelectorAll('tr')].find(r=>r.textContent.includes('QA Designer')).querySelector('a[href*=""/admin/seats?""]').getAttribute('href')"
 Navigate $roomLink "!!document.getElementById('seat-designer') && typeof window.CineSeatDesigner==='object' && !!document.getElementById('generate-layout')"
 Assert-Check ((Js "document.querySelectorAll('.designer-row').length===0") -eq $true) 'Empty room loads in creation mode'
 $null=Js "document.getElementById('layout-rows').value=1;document.getElementById('generate-layout').click();document.querySelector('.designer-row-label').click();document.getElementById('remove-row').click();true"
 Assert-Check ((Js "document.querySelectorAll('.designer-row').length===0") -eq $true) 'Delete sole unsaved row without minimum-row alert'
 $null=Js "document.getElementById('layout-rows').value=5;document.getElementById('layout-count').value=10;document.getElementById('generate-layout').click();window.qaRow=label=>[...document.querySelectorAll('.designer-row-label')].find(b=>b.textContent===label).click();true"
 Assert-Check ((Js "[...document.querySelectorAll('.designer-row-label')].map(b=>b.textContent).join(',')==='A,B,C,D,E'") -eq $true) 'Generate A through E directly on the canvas'
 Assert-Check ((Js "document.querySelectorAll('.designer-seat').length===50 && [...document.querySelectorAll('.designer-seat')].every(s=>s.classList.contains('seat-NORMAL'))") -eq $true) 'A-E with ten positions each initializes all 50 seats as NORMAL'
 foreach($config in @(@('A',12,'NORMAL'),@('B',12,'NORMAL'),@('C',14,'VIP'),@('D',14,'VIP'),@('E',8,'COUPLE'))){
  $null=Js ("qaRow('"+$config[0]+"');document.getElementById('row-count').value="+$config[1]+";document.getElementById('row-type').value='"+$config[2]+"';document.getElementById('apply-row').click();true")
 }
 $geometry=Js @'
 (() => {
  const screen=document.querySelector('.designer-screen').getBoundingClientRect(),axis=screen.x+screen.width/2;
  const rows=[...document.querySelectorAll('.designer-row')].map(r=>{
   const label=r.querySelector('.designer-row-label').getBoundingClientRect();
   const seats=[...r.querySelectorAll('.designer-seat')].map(s=>s.getBoundingClientRect());
   return {row:r.dataset.row,labelX:label.x,labelY:label.y+label.height/2,
    seatY:seats[0].y+seats[0].height/2,center:((seats[0].x+seats[0].width/2)+(seats.at(-1).x+seats.at(-1).width/2))/2};
  });
  return {axis,rows,centered:rows.every(r=>Math.abs(r.center-axis)<1),
   labelsAligned:rows.every(r=>Math.abs(r.labelX-rows[0].labelX)<1),
   labelsCentered:rows.every(r=>Math.abs(r.labelY-r.seatY)<2)};
 })()
'@
 Assert-Check $geometry.centered '12/14/8-seat rows align with screen center in real Edge'
 Assert-Check $geometry.labelsAligned 'All row labels share the same left column'
 Assert-Check $geometry.labelsCentered 'Row labels are vertically centered against seats'
 $null=Js "qaRow('C');true"
 Assert-Check ((Js "Math.abs(document.getElementById('row-count').getBoundingClientRect().top-document.getElementById('row-first').getBoundingClientRect().top)<1") -eq $true) 'Seat count and starting number inputs align horizontally'
 $signature="[...document.querySelectorAll('.designer-row')].flatMap(r=>[...r.querySelectorAll('.designer-seat')].map(s=>r.dataset.row+':'+s.dataset.number+':'+[...s.classList].filter(c=>c.startsWith('seat-')).sort().join(','))).join('|')"
 $before=Js $signature
 $null=Js "document.getElementById('designer-form').requestSubmit();true"
 Wait-Js "location.hash==='#designer-saved' && document.querySelectorAll('#designer-seed [data-seat-id]').length===60 && document.querySelectorAll('.designer-seat').length===60"
 Assert-Check ((Js $signature) -eq $before) 'Saved layout matches draft seat positions, types and activity'
 $originalIds=@(Js "[...document.querySelectorAll('#designer-seed [data-seat-id]')].map(e=>e.dataset.seatId)")
 $null=Js @'
 window.qaRow=label=>[...document.querySelectorAll('.designer-row-label')].find(b=>b.textContent===label).click();
 document.querySelector('[data-seat-key="A:1"] .designer-seat').click();
 document.getElementById('seat-type').value='VIP';document.getElementById('apply-seat').click();
 qaRow('A');document.getElementById('row-count').value=14;document.getElementById('apply-row').click();
 qaRow('B');document.getElementById('row-count').value=10;document.getElementById('apply-row').click();
 qaRow('E');document.getElementById('remove-row').click();
 true
'@
 Assert-Check ((Js "document.querySelector('[data-seat-key=""A:1""] .designer-seat').classList.contains('seat-VIP')") -eq $true) 'Resizing existing row preserves individual VIP override'
 Assert-Check ((Js "document.querySelector('[data-seat-key=""B:12""] .designer-seat').classList.contains('seat-inactive')") -eq $true) 'Reducing existing row keeps removed tail gray'
 Assert-Check ((Js "[...document.querySelectorAll('.designer-row[data-row=""E""] .designer-seat')].every(s=>s.classList.contains('seat-inactive'))") -eq $true) 'Deleting saved row deactivates whole row in draft'
 $null=Js "document.getElementById('confirm-existing').checked=true;document.getElementById('designer-form').requestSubmit();true"
 Wait-Js "location.hash==='#designer-saved' && document.querySelectorAll('#designer-seed [data-seat-id]').length===62 && document.querySelectorAll('.designer-seat').length===62"
 $originalJson=ConvertTo-Json -InputObject $originalIds -Compress
 Assert-Check ((Js ("("+$originalJson+").every(id=>[...document.querySelectorAll('#designer-seed [data-seat-id]')].some(e=>e.dataset.seatId===id))")) -eq $true) 'All 60 existing IDs survive complete save'
 Assert-Check ((Js "[...document.querySelectorAll('#designer-seed [data-row=""E""]')].every(e=>e.dataset.active==='false')") -eq $true) 'Saved row removal persists inactive flags without deletion'
 Assert-Check ((Js "document.querySelector('[data-seat-key=""A:1""] .designer-seat').classList.contains('seat-VIP')") -eq $true) 'Individual override survives reload'
 $null=Js "document.getElementById('layout-start').value='A';document.getElementById('layout-rows').value=5;document.getElementById('layout-count').value=10;document.getElementById('generate-layout').click();true"
 Assert-Check ((Js "document.querySelector('[data-seat-key=""A:13""] .designer-seat').classList.contains('seat-inactive') && document.querySelector('[data-seat-key=""A:1""] .designer-seat').classList.contains('seat-VIP')") -eq $true) 'Layout generator resizes saved rows without losing IDs or custom types'
 $null=Js "document.querySelector('[data-seat-key=""A:2""] input').click();document.querySelector('[data-seat-key=""A:3""] input').click();document.getElementById('selection-type').value='COUPLE';document.getElementById('apply-selection').click();true"
 Assert-Check ((Js "['A:2','A:3'].every(key=>document.querySelector('[data-seat-key=""'+key+'""] .designer-seat').classList.contains('seat-COUPLE'))") -eq $true) 'Multi-seat selection updates the draft'
 $null=Js "window.scrollTo(0,0);true"
 $image=Cdp 'Page.captureScreenshot' @{format='png';captureBeyondViewport=$false}
 [IO.File]::WriteAllBytes((Join-Path $qaDir 'designer-desktop.png'),[Convert]::FromBase64String($image.data))
 $null=Cdp 'Emulation.setDeviceMetricsOverride' @{width=390;height=844;deviceScaleFactor=1;mobile=$true}
 $mobile=Js "document.querySelector('.designer-scroll').scrollWidth>document.querySelector('.designer-scroll').clientWidth"
 Assert-Check ($mobile -eq $true) 'Narrow viewport scrolls horizontally for large rooms'
 $checks | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $qaDir 'checks.json') -Encoding UTF8
 $geometry | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $qaDir 'geometry.json') -Encoding UTF8
 Write-Output ("Browser checks passed: "+$checks.Count)
}finally{
 if($socket){
  try{
   $close=[Text.Encoding]::UTF8.GetBytes('{"id":999999,"method":"Browser.close"}')
   $null=$socket.SendAsync([ArraySegment[byte]]::new($close),[Net.WebSockets.WebSocketMessageType]::Text,$true,[Threading.CancellationToken]::None).GetAwaiter().GetResult()
  }catch{}
  $socket.Dispose()
 }
 if($browser -and !$browser.HasExited){Stop-Process -Id $browser.Id -Force -ErrorAction SilentlyContinue}
 if($app -and !$app.HasExited){Stop-Process -Id $app.Id -Force -ErrorAction SilentlyContinue}
}
