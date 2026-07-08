sed -i 's/val sanitized = input.replace(Regex("[^0-9]"), "")/val sanitized = input.uppercase().replace("IR", "").replace(Regex("[^0-9]"), "")/g' app/src/main/java/com/example/ui/MainViewModel.kt
