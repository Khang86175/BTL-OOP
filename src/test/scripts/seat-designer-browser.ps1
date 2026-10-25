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
 $null=Js ([IO.File]::ReadAllText((Join-Path $workspaceRoot 'src/main/resources/static/js/seat-designer-model.js')))
 $null=Js ([IO.File]::ReadAllText((Join-Path $workspaceRoot 'src/test/resources/seat-designer-model.test.js')))
 $jsTests=Js 'CineSeatDesignerTestResults.length'
 Write-Output ("JavaScript model tests passed: "+$jsTests)
 $null=Cdp 'Emulation.setDeviceMetricsOverride' @{width=1600;height=1100;deviceScaleFactor=1;mobile=$false}
 Navigate '/login' "!!document.querySelector('input[name=username]')"
 $null=Js "document.querySelector('input[name=username]').value='admin@cineticket.test';document.querySelector('input[name=password]').value='DemoAdmin123!';document.querySelector('input[name=username]').form.requestSubmit();true"
 Wait-Js "!!document.querySelector('a[href=""/admin/rooms""]')"
 Navigate '/admin/rooms' "!!document.querySelector('a[href*=""/admin/seats?""]')"
 $roomLink=Js "document.querySelector('a[href*=""/admin/seats?""]').getAttribute('href')"
 Navigate $roomLink "!!document.getElementById('seat-manager') && !!window.CineSeatEditor"
 Assert-Check ((Js "document.querySelectorAll('#saved-seat-data [data-seat-id]').length===0 && document.querySelectorAll('.seat-box').length===40") -eq $true) 'Empty room has live 5x8 preview without saved seats'
 $null=Js "document.getElementById('create-layout-form').requestSubmit();true"
 Wait-Js "document.querySelectorAll('#saved-seat-data [data-seat-id]').length===40 && !!window.CineSeatEditor"
 Assert-Check ((Js "[...document.querySelectorAll('.seat-map-label')].map(e=>e.textContent).join(',')==='A,B,C,D,E'") -eq $true) 'Create 5x8 saves rows A through E'
 Assert-Check ((Js "[...document.querySelectorAll('.seat-map-row')].every(r=>[...r.querySelectorAll('.seat-box')].map(s=>s.dataset.number).join(',')==='1,2,3,4,5,6,7,8')") -eq $true) 'Each row has exactly seats one through eight'
 Assert-Check ((Js "[...document.querySelectorAll('.seat-box')].every(s=>s.dataset.type==='NORMAL' && s.dataset.active==='true')") -eq $true) 'All forty new seats are active NORMAL'
 Assert-Check ((Js "!document.querySelector('option[value=COUPLE]') && !document.body.textContent.includes('Gh\u1ebf \u0111\u00f4i')") -eq $true) 'No COUPLE option or label'
 Assert-Check ((Js "['row-gaps','row-first','row-panel','selection-type','generate-layout','reset-draft','designer-save'].every(id=>!document.getElementById(id)) && !document.querySelector('input[name=seatIds]') && document.querySelectorAll('#create-layout-form button[type=submit]').length===1") -eq $true) 'No aisle, custom start, bulk editing or separate draft controls'
 $original=Js "JSON.stringify([...document.querySelectorAll('#saved-seat-data [data-seat-id]')].map(s=>({...s.dataset})))"
 $null=Js "document.querySelector('.seat-box[data-row=A][data-number=""1""]').click();document.getElementById('edit-type').value='VIP';document.getElementById('seat-edit-form').requestSubmit();true"
 Wait-Js "!!window.CineSeatEditor && document.querySelector('.seat-box[data-row=A][data-number=""1""]').dataset.type==='VIP'"
 Assert-Check ((Js "document.querySelector('.seat-box[data-row=A][data-number=""1""]').classList.contains('seat-VIP')") -eq $true) 'A1 changes to VIP'
 $null=Js "document.querySelector('.seat-box[data-row=A][data-number=""1""]').click();document.getElementById('edit-type').value='NORMAL';document.getElementById('seat-edit-form').requestSubmit();true"
 Wait-Js "!!window.CineSeatEditor && document.querySelector('.seat-box[data-row=A][data-number=""1""]').dataset.type==='NORMAL'"
 Assert-Check ((Js "document.querySelector('.seat-box[data-row=A][data-number=""1""]').classList.contains('seat-NORMAL')") -eq $true) 'A1 changes back to NORMAL'
 $null=Js "document.querySelector('.seat-box[data-row=A][data-number=""2""]').click();document.getElementById('edit-active').checked=false;document.getElementById('seat-edit-form').requestSubmit();true"
 Wait-Js "!!window.CineSeatEditor && document.querySelector('.seat-box[data-row=A][data-number=""2""]').dataset.active==='false'"
 Navigate $roomLink "!!window.CineSeatEditor && document.querySelectorAll('#saved-seat-data [data-seat-id]').length===40"
 Assert-Check ((Js "document.querySelector('.seat-box[data-row=A][data-number=""2""]').dataset.active==='false'") -eq $true) 'Reload keeps A2 inactive'
 $null=Js "document.querySelector('.seat-box[data-row=A][data-number=""2""]').click();document.getElementById('edit-active').checked=true;document.getElementById('seat-edit-form').requestSubmit();true"
 Wait-Js "!!window.CineSeatEditor && document.querySelector('.seat-box[data-row=A][data-number=""2""]').dataset.active==='true'"
 Assert-Check ((Js "document.querySelectorAll('#saved-seat-data [data-seat-id]').length===40") -eq $true) 'Explicit reactivation keeps forty seats'
 # Persist mixed type/state before resizing, to prove resizing does not reset them.
 $null=Js "document.querySelector('.seat-box[data-row=A][data-number=""1""]').click();document.getElementById('edit-type').value='VIP';document.getElementById('seat-edit-form').requestSubmit();true"
 Wait-Js "!!window.CineSeatEditor && document.querySelector('.seat-box[data-row=A][data-number=""1""]').dataset.type==='VIP'"
 $null=Js "document.querySelector('.seat-box[data-row=A][data-number=""2""]').click();document.getElementById('edit-active').checked=false;document.getElementById('seat-edit-form').requestSubmit();true"
 Wait-Js "!!window.CineSeatEditor && document.querySelector('.seat-box[data-row=A][data-number=""2""]').dataset.active==='false'"
 $beforeGrowth=Js "JSON.stringify([...document.querySelectorAll('#saved-seat-data [data-seat-id]')].map(s=>({...s.dataset})))"
 $null=Js "document.getElementById('rowCount').value=6;document.getElementById('rowCount').dispatchEvent(new Event('input',{bubbles:true}));true"
 Assert-Check ((Js "document.querySelectorAll('.seat-box').length===48 && document.querySelectorAll('#saved-seat-data [data-seat-id]').length===40") -eq $true) 'Changing row count immediately previews 48 without writing'
 $null=Js "document.getElementById('create-layout-form').requestSubmit();true"
 Wait-Js "!!window.CineSeatEditor && document.querySelectorAll('#saved-seat-data [data-seat-id]').length===48"
 Assert-Check ((Js "document.querySelectorAll('.seat-box[data-row=F]').length===8") -eq $true) 'One update saves 6x8 as 48 seats'
 $null=Js "document.getElementById('seatsPerRow').value=10;document.getElementById('seatsPerRow').dispatchEvent(new Event('input',{bubbles:true}));true"
 Assert-Check ((Js "document.querySelectorAll('.seat-box').length===60 && document.querySelectorAll('#saved-seat-data [data-seat-id]').length===48 && ![...document.querySelectorAll('.seat-box')].some(s=>Number(s.dataset.number)>10)") -eq $true) 'Changing seat count previews exactly 6x10 with no overflow'
 $null=Js "document.getElementById('create-layout-form').requestSubmit();true"
 Wait-Js "!!window.CineSeatEditor && document.querySelectorAll('#saved-seat-data [data-seat-id]').length===60"
 Assert-Check ((Js ("("+$beforeGrowth+").every(old=>[...document.querySelectorAll('#saved-seat-data [data-seat-id]')].some(s=>JSON.stringify({...s.dataset})===JSON.stringify(old)))")) -eq $true) 'Growth preserves every original ID, type and active state'
 $null=Js "document.getElementById('rowCount').value=5;document.getElementById('seatsPerRow').value=8;document.getElementById('rowCount').dispatchEvent(new Event('input',{bubbles:true}));window.confirm=()=>false;document.getElementById('create-layout-form').requestSubmit();true"
 Assert-Check ((Js "document.querySelectorAll('.seat-box').length===40 && document.querySelectorAll('#saved-seat-data [data-seat-id]').length===60") -eq $true) 'Cancelling shrink confirmation leaves 60 saved seats'
 $null=Js "sessionStorage.setItem('qaConfirmations','0');window.confirm=()=>{sessionStorage.setItem('qaConfirmations',String(Number(sessionStorage.getItem('qaConfirmations'))+1));return true;};document.getElementById('create-layout-form').requestSubmit();true"
 Wait-Js "!!window.CineSeatEditor && document.querySelectorAll('#saved-seat-data [data-seat-id]').length===40"
 Assert-Check ((Js "sessionStorage.getItem('qaConfirmations')==='1' && document.getElementById('rowCount').value==='5' && document.getElementById('seatsPerRow').value==='8'") -eq $true) 'One confirmation directly shrinks to 5x8'
 Assert-Check ((Js ("JSON.stringify([...document.querySelectorAll('#saved-seat-data [data-seat-id]')].map(s=>({...s.dataset})))==="+($beforeGrowth|ConvertTo-Json -Compress))) -eq $true) 'Shrink preserves every retained ID, type and state'
 $null=Js "document.querySelector('.seat-box[data-row=A][data-number=""3""]').click();window.confirm=()=>false;document.getElementById('seat-delete-form').requestSubmit();true"
 Assert-Check ((Js "document.querySelectorAll('#saved-seat-data [data-seat-id]').length===40") -eq $true) 'Cancelling single-seat deletion does not write'
 $null=Js "window.confirm=()=>true;document.getElementById('seat-delete-form').requestSubmit();true"
 Wait-Js "!!window.CineSeatEditor && document.querySelectorAll('#saved-seat-data [data-seat-id]').length===39"
 Assert-Check ((Js "!document.querySelector('.seat-box[data-row=A][data-number=""3""]') && !!document.querySelector('.seat-box[data-row=A][data-number=""4""]')") -eq $true) 'Delete one seat without renumbering its neighbors'
 Navigate $roomLink "!!window.CineSeatEditor && document.querySelectorAll('#saved-seat-data [data-seat-id]').length===39"
 Assert-Check ((Js "!document.querySelector('.seat-box[data-row=A][data-number=""3""]') && document.querySelector('.seat-box[data-row=A][data-number=""2""]').dataset.active==='false'") -eq $true) 'Reload preserves deletion and inactive state'
 $geometry=Js "(()=>{const s=document.querySelector('.seat-map-screen .screen-label').getBoundingClientRect(),first=document.querySelector('.seat-box').getBoundingClientRect(),labels=[...document.querySelectorAll('.seat-map-label')].map(e=>e.getBoundingClientRect());return {screenAbove:s.bottom<first.top,labelsAligned:labels.every(e=>Math.abs(e.left-labels[0].left)<1)};})()"
 Assert-Check $geometry.screenAbove 'Screen is above seats'
 Assert-Check $geometry.labelsAligned 'Row labels align on the left'
 $image=Cdp 'Page.captureScreenshot' @{format='png';captureBeyondViewport=$false}
 [IO.File]::WriteAllBytes((Join-Path $qaDir 'seats-direct-save.png'),[Convert]::FromBase64String($image.data))
 $null=Cdp 'Emulation.setDeviceMetricsOverride' @{width=390;height=844;deviceScaleFactor=1;mobile=$true}
 Assert-Check ((Js "document.querySelector('.seat-map-scroll').scrollWidth>document.querySelector('.seat-map-scroll').clientWidth") -eq $true) 'Narrow seat map scrolls horizontally'
 $null=Js "window.confirm=()=>true;document.getElementById('delete-layout-form').requestSubmit();true"
 Wait-Js "!!window.CineSeatEditor && document.querySelectorAll('#saved-seat-data [data-seat-id]').length===0"
 Assert-Check ((Js "!!document.getElementById('create-layout-form') && !document.getElementById('delete-layout-form') && document.querySelectorAll('.seat-box').length===40") -eq $true) 'Delete all unreferenced seats returns to empty-room live preview'
 Navigate $roomLink "!!window.CineSeatEditor && document.querySelectorAll('#saved-seat-data [data-seat-id]').length===0"
 Assert-Check ((Js "document.querySelectorAll('.seat-box[data-seat-id]').length===0") -eq $true) 'Reload after clear never recreates database seats'
 # Showtime has independent NORMAL/VIP prices, with native browser validation.
 $null=Cdp 'Emulation.setDeviceMetricsOverride' @{width=1600;height=1100;deviceScaleFactor=1;mobile=$false}
 Navigate '/admin/showtimes/new' "!!document.getElementById('showtime-form') && !!document.getElementById('normalPrice')"
 Assert-Check ((Js "['normalPrice','vipPrice'].every(id=>{const e=document.getElementById(id);return e.required&&e.min==='0.01'&&e.step==='0.01';}) && !document.querySelector('input[name=basePrice]')") -eq $true) 'Showtime form exposes two required positive price fields and no legacy price input'
 Assert-Check ((Js "!document.getElementById('normalPrice').checkValidity() && !document.getElementById('vipPrice').checkValidity()") -eq $true) 'Both blank prices fail native validation'
 Assert-Check ((Js "['normalPrice','vipPrice'].every(id=>['0','-1','abc'].every(value=>{const e=document.getElementById(id);e.value=value;return !e.checkValidity();}))") -eq $true) 'Zero negative and nonnumeric prices fail browser validation'
 $null=Js "const movie=document.getElementById('movieId');movie.value=[...movie.options].find(o=>o.value&&!o.textContent.includes('\u0111\u00e3 \u1ea9n')).value;const room=document.getElementById('roomId');room.value=[...room.options].find(o=>o.value).value;document.getElementById('startTime').value=new Date(Date.now()+90*86400000).toISOString().slice(0,10)+'T10:00';document.getElementById('normalPrice').value='80000';document.getElementById('vipPrice').value='95000';true"
 $priceDate=Js "document.getElementById('startTime').value.split('T')[0]"
 $null=Js "document.getElementById('showtime-form').requestSubmit();true"
 Wait-Js "location.pathname==='/admin/showtimes' && [...document.querySelectorAll('tr[data-showtime-id]')].some(r=>Number(r.dataset.normalPrice)===80000&&Number(r.dataset.vipPrice)===95000)"
 Assert-Check ((Js "[...document.querySelectorAll('tr[data-showtime-id]')].some(r=>r.querySelector('.normal-price').textContent.includes('80,000.00')&&r.querySelector('.vip-price').textContent.includes('95,000.00'))") -eq $true) 'Creating a showtime saves and displays two distinct prices'
 $priceEdit=Js "[...document.querySelectorAll('tr[data-showtime-id]')].find(r=>Number(r.dataset.normalPrice)===80000&&Number(r.dataset.vipPrice)===95000).querySelector('a').getAttribute('href')"
 $priceId=Js "[...document.querySelectorAll('tr[data-showtime-id]')].find(r=>Number(r.dataset.normalPrice)===80000&&Number(r.dataset.vipPrice)===95000).dataset.showtimeId"
 Navigate $priceEdit "!!document.getElementById('normalPrice')"
 $null=Js "document.getElementById('normalPrice').value='85000';document.getElementById('showtime-form').requestSubmit();true"
 Wait-Js "location.pathname==='/admin/showtimes' && [...document.querySelectorAll('tr[data-showtime-id]')].some(r=>Number(r.dataset.normalPrice)===85000&&Number(r.dataset.vipPrice)===95000)"
 Assert-Check ((Js "[...document.querySelectorAll('tr[data-showtime-id]')].some(r=>Number(r.dataset.normalPrice)===85000&&Number(r.dataset.vipPrice)===95000)") -eq $true) 'Editing NORMAL leaves VIP unchanged'
 Navigate $priceEdit "!!document.getElementById('vipPrice')"
 $null=Js "document.getElementById('vipPrice').value='70000';document.getElementById('showtime-form').requestSubmit();true"
 Wait-Js "location.pathname==='/admin/showtimes' && [...document.querySelectorAll('tr[data-showtime-id]')].some(r=>Number(r.dataset.normalPrice)===85000&&Number(r.dataset.vipPrice)===70000)"
 Assert-Check ((Js "[...document.querySelectorAll('tr[data-showtime-id]')].some(r=>Number(r.dataset.normalPrice)===85000&&Number(r.dataset.vipPrice)===70000)") -eq $true) 'VIP can change independently and be cheaper than NORMAL'
 Navigate $priceEdit "!!document.getElementById('normalPrice')"
 Assert-Check ((Js "Number(document.getElementById('normalPrice').value)===85000&&Number(document.getElementById('vipPrice').value)===70000") -eq $true) 'Reloading edit form retains both stored prices'
 Navigate ("/showtimes/"+$priceId) "!!document.querySelector('.normal-price') && !!document.querySelector('.vip-price')"
 Assert-Check ((Js "document.querySelector('.normal-price').textContent.includes('85,000.00')&&document.querySelector('.vip-price').textContent.includes('70,000.00')") -eq $true) 'Guest showtime detail displays both configured prices'
 Navigate ("/showtimes?date="+$priceDate) "!!document.querySelector('article .normal-price')"
 Assert-Check ((Js "[...document.querySelectorAll('article')].some(r=>r.querySelector('.normal-price').textContent.includes('85,000.00')&&r.querySelector('.vip-price').textContent.includes('70,000.00'))") -eq $true) 'Guest schedule list displays both configured prices'
 $priceImage=Cdp 'Page.captureScreenshot' @{format='png';captureBeyondViewport=$false}
 [IO.File]::WriteAllBytes((Join-Path $qaDir 'showtime-prices.png'),[Convert]::FromBase64String($priceImage.data))
 $checks | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $qaDir 'checks.json') -Encoding UTF8
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
