package app.swarsetu.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record NewCollege(
        @NotBlank @Size(max = 150) String collegeName,
        @Size(max = 200) String affiliation,
        @NotBlank @Size(max = 80) String city,
        @Size(max = 80) String country,
        @NotBlank @Size(max = 60) String firstName,
        @Size(max = 60) String lastName,
        @Size(max = 60) String username,
        @NotBlank @Size(max = 120) String email,
        @Size(max = 15) String mobile,
        @Size(max = 5) String countryCode,
        @NotBlank String password) {}

    public record Join(
        @Size(max = 12) String collegeCode,
        @Size(max = 150) String collegeName,
        @NotBlank String role,
        @NotBlank @Size(max = 60) String firstName,
        @Size(max = 60) String lastName,
        @Size(max = 60) String username,
        @NotBlank @Size(max = 120) String email,
        @Size(max = 15) String mobile,
        @Size(max = 5) String countryCode,
        @Size(max = 80) String city,
        @Size(max = 80) String country,
        @NotBlank String password) {}

    public record Verify(@NotBlank String verificationId, String emailCode, String mobileCode) {}

    public record Login(@NotBlank @Size(max = 120) String identifier, @NotBlank @Size(max = 128) String password) {}

    public record Refresh(@NotBlank String refreshToken) {}

    public record Forgot(@NotBlank @Size(max = 120) String identifier) {}

    public record Reset(@NotBlank String verificationId, String emailCode, String mobileCode, @NotBlank String newPassword) {}

    public record ChangePassword(@NotBlank String currentPassword, @NotBlank String newPassword) {}

    public record Profile(@NotBlank @Size(max = 60) String firstName, @Size(max = 60) String lastName,
                          @Size(max = 30) String altContact, @Size(max = 80) String city, @Size(max = 80) String country) {}

    public record ChangeContact(@Size(max = 120) String email, @Size(max = 15) String mobile, @NotBlank String password) {}
}
