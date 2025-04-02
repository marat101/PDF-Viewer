## Android PDF Viewer

Library for displaying PDF documents on Android. There are basic gestures for scrolling and zooming without loss of quality due to rendering fragments of pages that are visible on the screen. There are also horizontal and vertical layouts.

## Preview
https://github.com/user-attachments/assets/b9466a94-9e0e-4dc3-9517-9e0956199a52

## Integration

Add to build.gradle:
``` kotlin
implementation("io.github.marat101:pdf-viewer:1.0.0-alpha")
```
## Using
``` kotlin
@Composable
fun Example(
    modifier: Modifier = Modifier,
    uri: Uri
) {
    val state = rememberReaderLayoutState(
        minZoom = 0.3f,
        maxZoom = 10f,
        uri = uri
    )
    ReaderLayout(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
        layoutState = state,
    ) {}
}
```

Scroll to page by index
``` kotlin
state.positionsState.scrollToPage(pageIndex)
```

Change layout orientation
``` kotlin
state.positionsState.setOrientation(newOrientation)
```



