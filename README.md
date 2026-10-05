# paper-lib

A small Kotlin library and CLI that measures a sheet of paper and draws it.

The measurements are the ones that matter when you spec or buy stock: trim,
area, basis weight, caliper, sheet mass, ream mass and stack height. The
renderer shades a sheet with ANSI truecolour so you can see the result in a
terminal instead of trusting a table.

No dependencies beyond the Kotlin standard library.

## Measurements

Basis weight is grams per square metre, so a sheet's mass is just its area
scaled by that fraction:

```
mass = (width_mm × height_mm / 1_000_000) × gsm
```

Caliper follows from density. 80 g/m² paper at 0.80 g/cm³ occupies 100 cm³ per
square metre, which is 0.1 mm thick:

```
caliper_µm = gsm / density
```

A standard ISO 500 ream is 500 sheets, which makes A4 80 gsm about 2.49 kg and
50 mm tall.

## Usage

```console
$ ./gradlew run
$ ./gradlew run --args="--size A3 --gsm 250"
$ ./gradlew run --args="--stock newsprint --landscape --kg 3"
$ ./gradlew run --args="--help"
```

From the runnable JAR:

```console
$ java -jar build/libs/paper-lib-0.1.0-all.jar --plain
```

## Library

```kotlin
val sheet = PaperSheet.standard()                    // A4, 80 g/m² copy
sheet.mass                                           // 4.99 g
sheet.thickness                                      // 100 µm
PaperReam.of(sheet).totalMass                         // 2.49 kg
PaperReam.forKilograms(sheet, targetKg = 3.0)         // ream by shipping weight
```

## Stock presets

| Preset | Basis | Density | Brightness | Finish |
| --- | --- | --- | --- | --- |
| copy | 80 g/m² | 0.80 g/cm³ | 84 | smooth |
| premium | 120 g/m² | 0.78 g/cm³ | 92 | smooth |
| offset | 90 g/m² | 0.82 g/cm³ | 80 | linen |
| newsprint | 45 g/m² | 0.65 g/cm³ | 58 | newsprint |
| recycled | 100 g/m² | 0.72 g/cm³ | 72 | recycled |
| board | 250 g/m² | 0.85 g/cm³ | 74 | cardboard |

`--gsm` picks the nearest preset, so the renderer shades with something close to
what you asked for.

## Layout

```
src/main/kotlin/dev/paperlib/
  Units.kt        value classes for the measured quantities
  PaperSize.kt    trim sizes and areas
  PaperStock.kt   basis weight, density, finish, derived caliper
  PaperSheet.kt   one sheet and a ream of it
  render/         ANSI renderer and the optics behind the colours
  cli/            argument parsing and the report
```

## CI

`.github/workflows/ci.yml` runs on every push and pull request: it validates the
Gradle wrapper JAR, builds with JDK 21, runs the test suite, renders several
sizes and stocks to catch runtime breakage, and uploads the runnable JAR as a
build artifact.