param(
    [Parameter(Mandatory = $true)]
    [string]$InputDoc,

    [Parameter(Mandatory = $true)]
    [string]$OutputPdf
)

$InputPath = [System.IO.Path]::GetFullPath($InputDoc)
$OutputPath = [System.IO.Path]::GetFullPath($OutputPdf)
$OutputDir = [System.IO.Path]::GetDirectoryName($OutputPath)

if (-not (Test-Path -LiteralPath $InputPath)) {
    throw "Input document was not found: $InputPath"
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$WordApp = $null
$Document = $null
try {
    $WordApp = New-Object -ComObject Word.Application
    $WordApp.Visible = $false
    $WordApp.DisplayAlerts = 0
    $Document = $WordApp.Documents.Open($InputPath, $false, $true)
    $Document.Fields.Update() | Out-Null
    foreach ($StoryRange in $Document.StoryRanges) {
        $CurrentRange = $StoryRange
        while ($null -ne $CurrentRange) {
            $CurrentRange.Fields.Update() | Out-Null
            $CurrentRange = $CurrentRange.NextStoryRange
        }
    }
    $Document.ExportAsFixedFormat($OutputPath, 17)
}
finally {
    if ($null -ne $Document) {
        $Document.Close(0)
        [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($Document)
    }
    if ($null -ne $WordApp) {
        $WordApp.Quit()
        [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($WordApp)
    }
    [System.GC]::Collect()
    [System.GC]::WaitForPendingFinalizers()
}

Write-Output "Exported QA PDF: $OutputPath"
