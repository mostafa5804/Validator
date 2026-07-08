                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (validCount > 0 || invalidCount > 0) {
                    StatsSection(validCount, invalidCount)
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                if (history.isNotEmpty()) {
                    HistorySection(history, context)
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}
