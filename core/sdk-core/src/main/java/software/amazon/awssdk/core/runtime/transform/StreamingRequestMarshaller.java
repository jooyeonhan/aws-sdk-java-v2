/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.core.runtime.transform;

import static software.amazon.awssdk.http.Header.CONTENT_TYPE;
import static software.amazon.awssdk.utils.Validate.paramNotNull;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Augments a {@link Marshaller} to add contents for a streamed request.
 *
 * @param <T> Type of POJO being marshalled.
 */
@SdkProtectedApi
public final class StreamingRequestMarshaller<T> implements Marshaller<T> {

    private final Marshaller<T> delegateMarshaller;
    private final RequestBody requestBody;
    private final boolean requiresLength;
    private final boolean transferEncoding;
    private final boolean useHttp2;

    /**
     * @param delegate    POJO marshaller (for path/query/header members).
     * @param requestBody {@link RequestBody} representing HTTP contents.
     */
    @Deprecated
    public StreamingRequestMarshaller(Marshaller<T> delegate, RequestBody requestBody) {
        this.delegateMarshaller = paramNotNull(delegate, "delegate");
        this.requestBody = paramNotNull(requestBody, "requestBody");
        this.requiresLength = false;
        this.transferEncoding = false;
        this.useHttp2 = false;
    }

    private StreamingRequestMarshaller(Builder builder) {
        this.delegateMarshaller = builder.delegateMarshaller;
        this.requestBody = builder.requestBody;
        this.requiresLength = builder.requiresLength;
        this.transferEncoding = builder.transferEncoding;
        this.useHttp2 = builder.useHttp2;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public SdkHttpFullRequest marshall(T in) {
        SdkHttpFullRequest.Builder marshalled = delegateMarshaller.marshall(in).toBuilder();
        marshalled.contentStreamProvider(requestBody.contentStreamProvider());
        String contentType = marshalled.firstMatchingHeader(CONTENT_TYPE)
                                       .orElse(null);
        if (StringUtils.isEmpty(contentType)) {
            marshalled.putHeader(CONTENT_TYPE, requestBody.contentType());
        }

        // Currently, SDK always require content length in RequestBody. So we always
        // send Content-Length header for sync APIs
        // This change will be useful if SDK relaxes the content-length requirement in RequestBody
        StreamingMarshallerUtil.addHeaders(marshalled, Optional.of(requestBody.contentLength()),
                                           requiresLength, transferEncoding, useHttp2);

        return marshalled.build();
    }

    /**
     * Builder class to build {@link StreamingRequestMarshaller} object.
     */
    public static final class Builder {
        private Marshaller delegateMarshaller;
        private RequestBody requestBody;
        private boolean requiresLength = Boolean.FALSE;
        private boolean transferEncoding = Boolean.FALSE;
        private boolean useHttp2 = Boolean.FALSE;

        private Builder() {
        }

        /**
         * @param delegateMarshaller POJO marshaller (for path/query/header members)
         * @return This object for method chaining
         */
        public Builder delegateMarshaller(Marshaller delegateMarshaller) {
            this.delegateMarshaller = delegateMarshaller;
            return this;
        }

        /**
         * @param requestBody {@link RequestBody} representing the HTTP payload
         * @return This object for method chaining
         */
        public Builder requestBody(RequestBody requestBody) {
            this.requestBody = requestBody;
            return this;
        }

        /**
         * @param requiresLength boolean value indicating if Content-Length header is required in the request
         * @return This object for method chaining
         */
        public Builder requiresLength(boolean requiresLength) {
            this.requiresLength = requiresLength;
            return this;
        }

        /**
         * @param transferEncoding boolean value indicating if Transfer-Encoding: chunked header is required in the request
         * @return This object for method chaining
         */
        public Builder transferEncoding(boolean transferEncoding) {
            this.transferEncoding = transferEncoding;
            return this;
        }

        /**
         * @param useHttp2 boolean value indicating if request uses HTTP 2 protocol
         * @return This object for method chaining
         */
        public Builder useHttp2(boolean useHttp2) {
            this.useHttp2 = useHttp2;
            return this;
        }

        public <T> StreamingRequestMarshaller<T> build() {
            return new StreamingRequestMarshaller<>(this);
        }
    }
}
