# UI Package

Contains Screens (Composables) and ViewModels, grouped by feature.
- `navigation`: NavHost, typed routes
- `theme`: Material 3 theme
- `common`: UiState, shared components
- other features: dashboard, quickadd, goal, etc.

UI never imports data. All business logic rules live in the domain layer.
