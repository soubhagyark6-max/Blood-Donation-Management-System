# Checks that every frontend file referenced by the pages exists.
# Run from the project root in PowerShell:   powershell -ExecutionPolicy Bypass -File tests\frontend-tests\check-frontend-files.ps1
$root = Resolve-Path "$PSScriptRoot\..\.."
$missing = 0
$required = @(
  "frontend\pages\home.html","frontend\pages\login.html","frontend\pages\register-choice.html",
  "frontend\pages\register-donor.html","frontend\pages\register-recipient.html","frontend\pages\faq.html",
  "frontend\pages\dashboard\staff.html","frontend\pages\dashboard\donor.html","frontend\pages\dashboard\recipient.html",
  "frontend\pages\donors\list.html","frontend\pages\requests\list.html","frontend\pages\inventory\index.html",
  "frontend\pages\camps\list.html","frontend\pages\reports\index.html","frontend\pages\admin\users.html",
  "frontend\components\layout.html","frontend\css\lifelink.css","frontend\js\app.js","frontend\js\live-stats.js",
  "frontend\images\logo.svg","frontend\images\hero-art.svg"
)
foreach ($f in $required) {
  if (Test-Path (Join-Path $root $f)) { Write-Host "OK      $f" } else { Write-Host "MISSING $f" -ForegroundColor Red; $missing++ }
}
if ($missing -eq 0) { Write-Host "All frontend files found." -ForegroundColor Green } else { Write-Host "$missing file(s) missing." -ForegroundColor Red; exit 1 }
