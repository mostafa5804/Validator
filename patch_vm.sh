sed -i 's/val sanitized = input.replace("\\n", "").replace(" ", "").replace("-", "")/val sanitized = input.replace(Regex("[^0-9]"), "")/g' app/src/main/java/com/example/ui/MainViewModel.kt
