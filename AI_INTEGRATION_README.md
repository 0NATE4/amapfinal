# EzyMap AI Integration - DeepSeek AI Enhancement

## Overview

EzyMap now features advanced AI-powered search capabilities using DeepSeek AI to bridge the gap between English and Chinese for AMap API queries. This integration enables foreigners in China to search for locations using natural English language, pinyin, or Chinese characters.

## Features

### 🤖 AI-Enhanced Search
- **English to Chinese Translation**: Automatically translates English queries to Chinese for AMap API
- **Pinyin Support**: Converts pinyin input to Chinese characters
- **Natural Language Processing**: Handles queries like "Where can I find dumplings?"
- **Multi-Keyword Search**: Generates multiple relevant search keywords for better results
- **Context-Aware**: Uses user location to provide more relevant results

### 🔍 Search Examples

| Input | AI Processing | AMap Search Keywords |
|-------|---------------|---------------------|
| "dumplings" | English → Chinese | ["饺子", "饺子店", "中餐"] |
| "Where can I find the underground market?" | Natural language → Keywords | ["地下市场", "地下商城", "购物中心"] |
| "beijing hutong" | Pinyin → Chinese | ["北京胡同", "胡同", "老北京"] |
| "星巴克" | Chinese (no change) | ["星巴克", "咖啡", "咖啡店"] |

## Technical Implementation

### Architecture

```
User Input → AIEnhancedSearchManager → DeepSeekAIService → AMap API → Results
```

### Key Components

1. **DeepSeekAIService** (`app/src/main/java/com/example/amap/ai/DeepSeekAIService.kt`)
   - Handles API communication with DeepSeek AI
   - Processes queries and generates search keywords
   - Provides fallback functionality

2. **AIEnhancedSearchManager** (`app/src/main/java/com/example/amap/search/AIEnhancedSearchManager.kt`)
   - Orchestrates AI processing and AMap search
   - Performs multi-keyword searches
   - Handles result aggregation and deduplication

3. **SearchUIHandler** (Enhanced)
   - Shows AI processing indicators
   - Provides user feedback during AI processing
   - Displays AI processing results

### API Configuration

The DeepSeek AI integration uses:
- **API Key**: `sk-f4ad3811b3c3417c8ad8fea2cdc7cead`
- **Base URL**: `https://api.deepseek.com/v1/`
- **Model**: `deepseek-chat`
- **Max Tokens**: 1000
- **Temperature**: 0.7

## Usage

### For Users

1. **English Queries**: Simply type in English
   ```
   "dumplings" → AI translates to "饺子" and searches
   ```

2. **Natural Language**: Ask questions naturally
   ```
   "Where can I find a good coffee shop?" → AI extracts relevant keywords
   ```

3. **Pinyin Input**: Type pinyin for Chinese characters
   ```
   "beijing hutong" → AI converts to "北京胡同"
   ```

4. **Chinese Input**: Works directly with Chinese characters
   ```
   "星巴克" → Searches directly
   ```

### For Developers

#### Adding AI Processing to Search

```kotlin
// Initialize AI search manager
val aiSearchManager = AIEnhancedSearchManager(
    context = this,
    onSearchResult = { poiItems, success, message, aiInfo ->
        // Handle search results with AI info
    },
    onAIProcessing = { isProcessing ->
        // Show/hide AI processing indicator
    }
)

// Perform AI-enhanced search
aiSearchManager.performAISearch("dumplings", userLocation)
```

#### Customizing AI Prompts

Modify the prompt in `DeepSeekAIService.buildPrompt()` to customize AI behavior:

```kotlin
private fun buildPrompt(userQuery: String, userLocation: Location?): String {
    // Customize the prompt for your specific use case
    return """
        You are an AI assistant helping foreigners in China find locations...
        // Your custom prompt here
    """.trimIndent()
}
```

## Error Handling

The AI integration includes robust error handling:

1. **API Failures**: Falls back to direct AMap search
2. **Network Issues**: Graceful degradation to original search
3. **Invalid Responses**: Uses fallback query processing
4. **Timeout Handling**: Configurable timeouts for API calls

## Testing

Run the AI integration tests:

```bash
./gradlew test --tests "com.example.amap.ai.DeepSeekAIServiceTest"
```

## Dependencies

The AI integration requires these additional dependencies:

```kotlin
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.google.code.gson:gson:2.10.1")
```

## Security

- API key is stored in `Constants.ApiKeys.DEEPSEEK_API_KEY`
- Network requests use HTTPS
- API key should be secured in production builds

## Future Enhancements

1. **Caching**: Cache AI responses for common queries
2. **Offline Mode**: Fallback to local translation dictionaries
3. **Voice Input**: Speech-to-text with AI processing
4. **Personalization**: Learn from user search patterns
5. **Multi-language Support**: Support for other languages beyond English

## Troubleshooting

### Common Issues

1. **AI Processing Fails**: Check internet connection and API key validity
2. **No Results**: Verify AMap API key and location permissions
3. **Slow Response**: AI processing may take 2-5 seconds depending on query complexity

### Debug Mode

Enable debug logging to troubleshoot AI processing:

```kotlin
Log.d("DeepSeekAI", "Processing query: $userQuery")
Log.d("DeepSeekAI", "AI response: $aiProcessedQuery")
```

## Contributing

When adding new AI features:

1. Update the AI prompt in `DeepSeekAIService`
2. Add corresponding tests
3. Update this documentation
4. Test with various input types (English, Chinese, Pinyin)

---

**Note**: This AI integration significantly enhances the user experience for foreigners in China by providing natural language search capabilities while maintaining compatibility with the existing AMap infrastructure. 