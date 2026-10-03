package pe.smartcash.cash.subscription.interfaces.rest.resources;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body de {@code POST /api/v1/subscriptions/premium}. {@code cardToken} es el token que generó
 * el checkout de Culqi en el navegador. Los límites de {@code payer} son los que Culqi exige al
 * crear un cliente: validarlos acá da un 400 claro en vez de un error genérico del proveedor.
 * {@code authentication3DS} viaja solo en el reintento, después de que el banco pidió 3DS.
 */
public record PayPremiumResource(
    @NotBlank String planCode,
    @NotBlank String cardToken,
    @NotNull @Valid PayerResource payer,
    @AssertTrue(message = "hay que aceptar los términos y condiciones") boolean acceptedTerms,
    @Valid ThreeDSecureResource authentication3DS) {

  public record PayerResource(
      // Culqi solo acepta letras y espacios en nombres y apellidos.
      @NotBlank @Size(min = 2, max = 50) @Pattern(regexp = "^[\\p{L} ]+$") String firstName,
      @NotBlank @Size(min = 2, max = 50) @Pattern(regexp = "^[\\p{L} ]+$") String lastName,
      @NotBlank @Email @Size(min = 5, max = 50) String email,
      @NotBlank @Pattern(regexp = "^[0-9]{5,15}$") String phoneNumber,
      @NotBlank @Size(min = 5, max = 100) String address,
      @NotBlank @Size(min = 2, max = 30) String addressCity,
      // ISO 3166-1 alfa-2; opcional, por defecto PE.
      @Pattern(regexp = "^[A-Z]{2}$") String countryCode) {}

  public record ThreeDSecureResource(
      String eci, String xid, String cavv, @NotBlank String protocolVersion, String directoryServerTransactionId) {}
}
