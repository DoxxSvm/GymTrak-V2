Get-ChildItem -Path "e:\jay\GymOwner\composeApp\src" -Filter "*.kt" -Recurse | ForEach-Object {
    $path = $_.FullName
    $c = Get-Content $path
    $c = $c -replace 'package gym\.trak\.studio', 'package `in`.gym.trak.studio'
    $c = $c -replace 'import gym\.trak\.studio', 'import `in`.gym.trak.studio'
    $c | Set-Content $path
}
