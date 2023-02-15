package com.sudoplatform.sudositereputation.transformers

import com.amazonaws.services.cognitoidentity.model.NotAuthorizedException
import com.sudoplatform.sudositereputation.SudoSiteReputationException
import com.sudoplatform.sudositereputation.s3.S3Exception
import java.io.IOException
import java.util.concurrent.CancellationException

internal object SudoSiteReputationExceptionTransformer {

    /**
     * Interpret an exception from SudoUserClient or S3 and map it to an exception
     * declared in this SDK's API that the caller is expecting.
     *
     * @param exception The exception from the secure value client.
     * @return The exception mapped to [SudoSiteReputationException]
     * or [CancellationException]
     */
    internal fun interpretException(exception: Throwable): Throwable {
        return when (exception) {
            is CancellationException, // Never wrap or reinterpret Kotlin coroutines cancellation exception
            is SudoSiteReputationException -> exception
            is S3Exception.MetadataException -> SudoSiteReputationException.DataFormatException(cause = exception)
            is S3Exception -> throw SudoSiteReputationException.FailedException(cause = exception)
            is NotAuthorizedException -> throw SudoSiteReputationException.UnauthorizedUserException(cause = exception)
            is IOException -> throw SudoSiteReputationException.FailedException(cause = exception)
            else -> SudoSiteReputationException.UnknownException(exception)
        }
    }
}
