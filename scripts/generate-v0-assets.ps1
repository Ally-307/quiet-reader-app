param(
  [string]$OutputDirectory = "C:\Users\lenovo\Desktop\workspace\project_053_android-reader\src\prototype\assets"
)

Add-Type -AssemblyName System.Drawing
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null

$width = 600
$height = 900

function New-Color {
  param([string]$Hex, [int]$Alpha = 255)
  $clean = $Hex.TrimStart('#')
  $red = [Convert]::ToInt32($clean.Substring(0, 2), 16)
  $green = [Convert]::ToInt32($clean.Substring(2, 2), 16)
  $blue = [Convert]::ToInt32($clean.Substring(4, 2), 16)
  return [System.Drawing.Color]::FromArgb($Alpha, $red, $green, $blue)
}

function Add-PaperTexture {
  param(
    [System.Drawing.Graphics]$Graphics,
    [System.Random]$Random,
    [string]$Ink = "#5E5B54"
  )

  for ($index = 0; $index -lt 1100; $index++) {
    $alpha = $Random.Next(4, 15)
    $color = New-Color -Hex $Ink -Alpha $alpha
    $brush = [System.Drawing.SolidBrush]::new($color)
    $x = $Random.Next(0, $width)
    $y = $Random.Next(0, $height)
    $size = $Random.Next(1, 4)
    $Graphics.FillEllipse($brush, $x, $y, $size, $size)
    $brush.Dispose()
  }

  for ($index = 0; $index -lt 70; $index++) {
    $alpha = $Random.Next(5, 13)
    $pen = [System.Drawing.Pen]::new((New-Color -Hex $Ink -Alpha $alpha), 1)
    $y = $Random.Next(0, $height)
    $x = $Random.Next(0, $width - 80)
    $Graphics.DrawLine($pen, $x, $y, $x + $Random.Next(30, 120), $y)
    $pen.Dispose()
  }
}

function Save-Cover {
  param(
    [string]$Name,
    [string]$Background,
    [scriptblock]$Draw,
    [int]$Seed
  )

  $bitmap = [System.Drawing.Bitmap]::new($width, $height)
  $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
  $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $graphics.Clear((New-Color -Hex $Background))
  $random = [System.Random]::new($Seed)

  & $Draw $graphics $random
  Add-PaperTexture -Graphics $graphics -Random $random

  $path = Join-Path $OutputDirectory "$Name.png"
  $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
  $graphics.Dispose()
  $bitmap.Dispose()
}

Save-Cover -Name "cover-tide" -Background "#EEE8DC" -Seed 53 -Draw {
  param($graphics, $random)
  $navy = [System.Drawing.Pen]::new((New-Color "#263A46" 220), 18)
  $navy.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
  $navy.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
  for ($i = 0; $i -lt 6; $i++) {
    $rect = [System.Drawing.Rectangle]::new(-140 + ($i * 18), 430 + ($i * 45), 780, 300)
    $graphics.DrawArc($navy, $rect, 202, 138)
  }
  $sun = [System.Drawing.SolidBrush]::new((New-Color "#B63A31" 235))
  $graphics.FillEllipse($sun, 376, 142, 112, 112)
  $sun.Dispose()
  $navy.Dispose()
}

Save-Cover -Name "cover-camellia" -Background "#CCD2BF" -Seed 54 -Draw {
  param($graphics, $random)
  $branch = [System.Drawing.Pen]::new((New-Color "#30342E" 220), 13)
  $graphics.DrawBezier($branch, 80, 760, 150, 520, 360, 420, 500, 120)
  $graphics.DrawBezier($branch, 190, 570, 230, 410, 120, 330, 90, 220)
  $flower = [System.Drawing.SolidBrush]::new((New-Color "#A33A32" 210))
  $leaf = [System.Drawing.SolidBrush]::new((New-Color "#52634D" 190))
  foreach ($point in @(@(145,515),@(282,425),@(410,266),@(92,310))) {
    $graphics.FillEllipse($flower, $point[0], $point[1], 72, 72)
  }
  foreach ($point in @(@(205,515),@(330,340),@(126,390),@(398,190))) {
    $graphics.FillEllipse($leaf, $point[0], $point[1], 42, 86)
  }
  $leaf.Dispose()
  $flower.Dispose()
  $branch.Dispose()
}

Save-Cover -Name "cover-rain" -Background "#C9D8DD" -Seed 55 -Draw {
  param($graphics, $random)
  for ($i = 0; $i -lt 24; $i++) {
    $alpha = $random.Next(45, 125)
    $pen = [System.Drawing.Pen]::new((New-Color "#26465B" $alpha), $random.Next(2, 7))
    $x = 50 + ($i * 23)
    $offset = $random.Next(-30, 70)
    $graphics.DrawLine($pen, $x, 120 + $offset, $x - 80, 700 + $offset)
    $pen.Dispose()
  }
  $pool = [System.Drawing.SolidBrush]::new((New-Color "#F3EEE4" 155))
  $graphics.FillEllipse($pool, 112, 596, 386, 136)
  $pool.Dispose()
}

Save-Cover -Name "cover-latitude" -Background "#D2B75D" -Seed 56 -Draw {
  param($graphics, $random)
  $ink = [System.Drawing.SolidBrush]::new((New-Color "#272925" 230))
  $paper = [System.Drawing.SolidBrush]::new((New-Color "#F1E7CF" 220))
  $graphics.FillRectangle($ink, 80, 370, 118, 330)
  $graphics.FillRectangle($ink, 224, 270, 132, 430)
  $graphics.FillRectangle($ink, 382, 440, 126, 260)
  $graphics.FillRectangle($paper, 109, 415, 58, 92)
  $graphics.FillRectangle($paper, 261, 325, 58, 130)
  $graphics.FillRectangle($paper, 414, 488, 60, 80)
  $graphics.FillEllipse($paper, 86, 108, 164, 164)
  $paper.Dispose()
  $ink.Dispose()
}

Save-Cover -Name "cover-town" -Background "#D7B7B5" -Seed 57 -Draw {
  param($graphics, $random)
  $burgundy = [System.Drawing.SolidBrush]::new((New-Color "#713B3D" 220))
  $cream = [System.Drawing.SolidBrush]::new((New-Color "#F1E8DD" 235))
  for ($i = 0; $i -lt 4; $i++) {
    $x = 54 + ($i * 135)
    $roof = [System.Drawing.Point[]]@(
      [System.Drawing.Point]::new($x, 490 - ($i % 2) * 60),
      [System.Drawing.Point]::new($x + 62, 420 - ($i % 2) * 60),
      [System.Drawing.Point]::new($x + 124, 490 - ($i % 2) * 60)
    )
    $graphics.FillPolygon($burgundy, $roof)
    $graphics.FillRectangle($cream, $x + 15, 488 - ($i % 2) * 60, 94, 178 + ($i % 2) * 60)
    $graphics.FillRectangle($burgundy, $x + 48, 548 - ($i % 2) * 60, 32, 118)
  }
  $cream.Dispose()
  $burgundy.Dispose()
}

Save-Cover -Name "cover-stars" -Background "#ECE9DE" -Seed 58 -Draw {
  param($graphics, $random)
  $green = [System.Drawing.Pen]::new((New-Color "#335743" 220), 10)
  $graphics.DrawBezier($green, 300, 760, 274, 560, 360, 370, 292, 150)
  $leaf = [System.Drawing.SolidBrush]::new((New-Color "#58765C" 185))
  foreach ($point in @(@(205,565),@(315,470),@(214,372),@(322,275))) {
    $graphics.FillEllipse($leaf, $point[0], $point[1], 76, 38)
  }
  $star = [System.Drawing.SolidBrush]::new((New-Color "#B63A31" 225))
  foreach ($point in @(@(118,178),@(410,136),@(438,348),@(116,408))) {
    $graphics.FillEllipse($star, $point[0], $point[1], 28, 28)
  }
  $star.Dispose()
  $leaf.Dispose()
  $green.Dispose()
}

$texture = [System.Drawing.Bitmap]::new(256, 256)
$textureGraphics = [System.Drawing.Graphics]::FromImage($texture)
$textureGraphics.Clear([System.Drawing.Color]::Transparent)
$textureRandom = [System.Random]::new(5301)
for ($index = 0; $index -lt 1800; $index++) {
  $alpha = $textureRandom.Next(4, 17)
  $tone = if (($index % 3) -eq 0) { "#FFFFFF" } else { "#5D594F" }
  $brush = [System.Drawing.SolidBrush]::new((New-Color -Hex $tone -Alpha $alpha))
  $x = $textureRandom.Next(0, 256)
  $y = $textureRandom.Next(0, 256)
  $size = $textureRandom.Next(1, 3)
  $textureGraphics.FillRectangle($brush, $x, $y, $size, $size)
  $brush.Dispose()
}
$texture.Save((Join-Path $OutputDirectory "paper-noise.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$textureGraphics.Dispose()
$texture.Dispose()

Write-Output "Generated covers in $OutputDirectory"
