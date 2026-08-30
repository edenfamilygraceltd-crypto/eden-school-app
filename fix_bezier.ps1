$p = 'C:\Users\user\Desktop\eden-school-app\index.html'
$c = Get-Content -Raw -LiteralPath $p
$old = 'cubic-bezier(.34,1.56,.64,1)'
$new = 'cubic-bezier(.2,.8,.2,1)'
$count = ([regex]::Matches($c, [regex]::Escape($old))).Count
$c2 = $c.Replace($old, $new)
Set-Content -NoNewline -LiteralPath $p -Value $c2
Write-Output "Replaced $count occurrences"
