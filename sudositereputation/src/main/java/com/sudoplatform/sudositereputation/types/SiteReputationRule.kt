package com.sudoplatform.sudositereputation.types

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class SiteReputationRule(
    var host: String,
    var path: String,
) : Parcelable {
    constructor(urlString: String) : this("", "") {
        var uri = Uri.parse(urlString)
        if (uri.scheme == null) {
            uri = Uri.parse("scheme://$urlString")
        }
        host = uri.host ?: urlString
        path = uri.path ?: urlString
    }
}
