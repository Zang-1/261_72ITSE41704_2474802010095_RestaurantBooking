﻿# Opens each built report in Microsoft Word, fills the table of contents, the lists of figures and
# tables and the figure/table numbers, saves the DOCX and exports a PDF next to it.
# The English report is named as Guidelines §8 asks; a copy named StudentID_FullName_... is the form the
# other members rename with their own ID and name. The Vietnamese report is "Bản tiếng Việt".
#   powershell -ExecutionPolicy Bypass -File docs\report\export_pdf.ps1
param([string[]]$Files = @('2474802010095_KieuBaoGiang_ApplicationProgrammingProject.docx', 'Bản tiếng Việt.docx'))

$dir = Join-Path $PSScriptRoot 'output'
$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
try {
    foreach ($name in $Files) {
        $path = Join-Path $dir $name
        if (-not (Test-Path $path)) { Write-Host "skip $name (not built)"; continue }
        $doc = $word.Documents.Open($path)
        $doc.Fields.Update() | Out-Null                      # SEQ numbers first
        foreach ($toc in $doc.TablesOfContents) { $toc.Update() | Out-Null }
        foreach ($tof in $doc.TablesOfFigures) { $tof.Update() | Out-Null }
        $doc.Fields.Update() | Out-Null
        $doc.Save()
        $pdf = [IO.Path]::ChangeExtension($path, '.pdf')
        $doc.ExportAsFixedFormat($pdf, 17)                   # 17 = PDF
        Write-Host "$pdf  pages=$($doc.ComputeStatistics(2))"
        $doc.Close([ref]0)
    }
} finally {
    $word.Quit()
    [Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null
}

foreach ($ext in @('pdf', 'docx')) {
    $src = Join-Path $dir "2474802010095_KieuBaoGiang_ApplicationProgrammingProject.$ext"
    if (Test-Path $src) { Copy-Item $src (Join-Path $dir "StudentID_FullName_ApplicationProgrammingProject.$ext") -Force }
}
