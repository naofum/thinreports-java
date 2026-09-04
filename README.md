# thinreports-java

Overlay-style report (PDF) generator for Java, compatible with
[Thinreports](https://www.thinreports.org/) `.tlf` layout templates.

Rewritten from the prototype (`thinreports-dev`) to draw directly on the
Apache PDFBox 3.x low-level API — no external flow-layout engine. Each tlf
element is placed by its absolute coordinates.

- groupId / artifactId / version: `com.github.naofum:thinreports-java:1.0.0`
- Java 21
- Apache License 2.0

See `docs/specification.md` for the full specification, roadmap and open issues.

## Usage

```java
TRGenerator generator = new TRGenerator();
// Point the IPA fonts at your TTF files for CJK text:
generator.getSettings().setIpamincho("ipam.ttf");
generator.getSettings().setIpagochic("ipag.ttf");

Map<String, Object> data = new HashMap<>();
data.put("single_line_left", "Hello");

generator.newDocument("report.tlf", data);   // first page
generator.addPage("report.tlf", data);        // additional pages (optional)
generator.save("out.pdf");
generator.close();
```

## Supported elements

`text`, `text-block`, `image`, `image-block`, `rect` (incl. rounded corners),
`ellipse`, `line`, `list` (detail repetition with auto page-break) and
`page-number`.

Text supports word wrap (word-boundary aware, with CJK breaking), horizontal /
vertical alignment, font-style decoration (bold / italic / underline /
line-through) and color. Inline markup is supported inside text values:
`{color:#rrggbb}`, `*bold*`, `_italic_`, `__underline__` and
`__{n:}line-through__`; markup lines wrap across the element width like plain
text.

Value formatting supports `number`, `padding` and `datetime`. The `datetime`
format accepts Ruby strftime directives including `%Y %y %m %B %b %d %e %j %A
%a %H %I %M %S %L %p %Z %z` and `%%`.

Paper sizes: A0–A6, B0–B6 (ISO 216), Letter and Legal, in portrait or
landscape.

## Lists

A `list` element repeats its detail band per data row and breaks to a new page
automatically. The header is redrawn on every page; page-footer and footer
bands are supported with static values or per-page / grand-total callbacks.

```java
TRGenerator generator = new TRGenerator();
generator.newDocument("list.tlf", new HashMap<>());

for (Order o : orders) {
    Map<String, Object> row = new HashMap<>();
    row.put("name", o.getName());
    row.put("amount", o.getAmount());
    generator.addRow(row);                 // add a detail row
}

// Per-page subtotal in the page-footer band.
generator.setPageFooterCallback((pageNumber, pageRows) -> {
    double subtotal = pageRows.stream()
            .mapToDouble(r -> ((Number) r.get("amount")).doubleValue()).sum();
    return Map.of("page_subtotal", subtotal);
});

// Grand total in the footer band (computed from all rows).
generator.setFooterCallback(allRows -> {
    double total = allRows.stream()
            .mapToDouble(r -> ((Number) r.get("amount")).doubleValue()).sum();
    return Map.of("grand_total", total);
});

generator.save("out.pdf");
generator.close();
```

`page-number` elements resolve `{page}` and `{total}` placeholders after the
full page count (including list page-breaks) is known.

## Not yet implemented

- letter-spacing / line-height fine control
- low-frequency strftime directives (week numbers `%U` / `%W`, ISO week)
- pixel-diff comparison against the Ruby generator's output

## Build

```
mvn test
mvn package
```

## Examples (barcode / QR / chart)

Barcode, QR and chart support is not part of the core library — those are
demonstrated in the separate `examples/` module, which generates images with
barcode4j / zxing / jfreechart and feeds them into `image-block` elements.

```
mvn install            # install the core library locally first
cd examples
mvn test
```

