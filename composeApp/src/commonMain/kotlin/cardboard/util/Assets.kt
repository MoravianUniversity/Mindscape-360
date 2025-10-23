package cardboard.util

import okio.Source

expect fun getAssetURL(path: String): String

expect fun getAsset(path: String, context: Any?): Source
