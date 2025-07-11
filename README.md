# Android PDF-Viewer

Library for displaying PDF documents on Android. There are basic gestures for scrolling and zooming without loss of quality due to rendering fragments of pages that are visible on the screen. There are also horizontal and vertical layouts.

## Preview
<img src="https://github.com/user-attachments/assets/e3a3c920-020c-4d49-a289-8f8bac5d71aa" width="30%" alt="Preview">
<img src="https://github.com/user-attachments/assets/5f59d782-2670-4c82-99df-46aaa2007ec0" width="30%" alt="Preview">

## Integration

Add to build.gradle:
``` kotlin
implementation("io.github.marat101:pdf-viewer:1.0.0-alpha01")
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



