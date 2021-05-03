# compose-drag-n-design
A hackathon project for a UI sketch designer made in Compose.

This is a project to get more familiarized with Jetpack Compose.

It explores how to:
- implement a reusable drag-n-drop.
- create a UI based on a dynamic specification.

## Drag and drop

The drag and drop feature was tested first in the `DragAndDropScreen.kt`.
You can switch `MainActivity` to call that instead of the `BuildScreen.kt`.

## UI builder

- Still some changes are needed to separate better the model+events from the UI.
- Several approaches were tried.
- One particularity of the approach I finally use (a change in an element changes all its parents)
  forces the whole UI to recompose which is a desired effect as containers depend on their children for layout.
- Said in other way: performance is not an issue/concern in this very particular case.
- Also: don't take this code as gold-standard. I'm not (yet!) a Compose expert and some good practices might been missing.
