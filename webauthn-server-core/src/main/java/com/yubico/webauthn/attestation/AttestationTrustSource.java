// Copyright (c) 2018, Yubico AB
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this
//    list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.yubico.webauthn.attestation;

import com.yubico.internal.util.CollectionUtil;
import com.yubico.webauthn.data.ByteArray;
import java.security.cert.CertStore;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/** Abstraction of a repository which can look up trust roots for authenticator attestation. */
public interface AttestationTrustSource {

  /**
   * Attempt to look up attestation trust roots for an authenticator.
   *
   * <p>Note that it is possible for the same trust root to be used for different certificate
   * chains. For example, an authenticator vendor may make two different authenticator models, each
   * with its own attestation leaf certificate but both signed by the same attestation root
   * certificate. If a Relying Party trusts one of those authenticator models but not the other,
   * then its implementation of this method MUST return an empty set for the untrusted certificate
   * chain.
   *
   * @param attestationCertificateChain the attestation certificate chain for the authenticator.
   * @param aaguid the AAGUID of the authenticator, if available.
   * @return A set of attestation root certificates trusted to attest for this authenticator, if any
   *     are available. If no trust roots are found, or if this authenticator is not trusted, return
   *     an empty result. Implementations MAY reuse the same result object, or parts of it, for
   *     multiple calls of this method, even with different arguments, but MUST return an empty set
   *     of trust roots for authenticators that should not be trusted.
   */
  TrustRootsResult findTrustRoots(
      List<X509Certificate> attestationCertificateChain, Optional<ByteArray> aaguid);

  /**
   * A result of looking up attestation trust roots for a particular attestation statement. This
   * primarily consists of a set of trust root certificates, but may also include a {@link
   * CertStore} of additional CRLs and/or intermediate certificate to use during certificate path
   * validation, and may also disable certificate revocation checking for the relevant attestation
   * statement.
   */
  @Value
  @Builder(toBuilder = true)
  class TrustRootsResult {

    /**
     * A set of attestation root certificates trusted to certify the relevant attestation statement.
     * If the attestation statement is not trusted, or if no trust roots were found, this should be
     * an empty set.
     */
    @NonNull private final Set<X509Certificate> trustRoots;

    /**
     * A {@link CertStore} of additional CRLs and/or intermediate certificates to use during
     * certificate path validation, if any. This will not be used if {@link
     * TrustRootsResultBuilder#trustRoots(Set) trustRoots} is empty.
     *
     * <p>Any certificates included in this {@link CertStore} are NOT considered trusted; they will
     * be trusted only if they chain to any of the {@link TrustRootsResultBuilder#trustRoots(Set)
     * trustRoots}.
     *
     * <p>The default is <code>null</code>.
     */
    @Builder.Default private final CertStore certStore = null;

    /**
     * Whether certificate revocation should be checked during certificate path validation.
     *
     * <p>The default is <code>true</code>.
     */
    @Builder.Default private final boolean enableRevocationChecking = true;

    private TrustRootsResult(
        @NonNull Set<X509Certificate> trustRoots,
        CertStore certStore,
        boolean enableRevocationChecking) {
      this.trustRoots = CollectionUtil.immutableSet(trustRoots);
      this.certStore = certStore;
      this.enableRevocationChecking = enableRevocationChecking;
    }

    public Optional<CertStore> getCertStore() {
      return Optional.ofNullable(certStore);
    }

    public static TrustRootsResultBuilder.Step1 builder() {
      return new TrustRootsResultBuilder.Step1();
    }

    public static class TrustRootsResultBuilder {
      public static class Step1 {
        public TrustRootsResultBuilder trustRoots(@NonNull Set<X509Certificate> trustRoots) {
          return new TrustRootsResultBuilder().trustRoots(trustRoots);
        }
      }
    }
  }
}
