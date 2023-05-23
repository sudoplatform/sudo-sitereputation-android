## 2.0.2
- Updates the AWS version used to 2.62.0

## 2.0.1

### Enhancements
- Introduces local caching for AppSync responses

## 2.0.0

#### Enhancements
- Made the data model of the service response simpler, now only containing `ReputationStatus`

#### Upgrade Path
- To use the old responses, use the `LegacySudoSiteReputationClient`
- To use the new responses, use `SiteReputationClient`

#### Fixes
- Updated to GraphQL schema and AppSync responses
- Updated the internal data model and transformers in line with the new schema
- Renamed `RealtimeReputationClient` to `SiteReputationClient`
- Kept the old stuff but renamed to `Legacy` (breaking)


## 1.5.0
Released February 16, 2023

### Enhancements
- Introduces new RealtimeReputationClient that provides async site reputation

## 1.3.1
Released May 5, 2022

### New
- Updated dependencies and Kotlin version

### Fixed

- None

## 1.3.0
Released Nov 2, 2021

### New
- A wider selection of malicious domains will now be reported as malicious. This includes a large selection of phishing URLs.

### Fixed

- None

## 1.2.1
Released Aug 24, 2021

### New
- Exposed Entitlement Name
- Updated Tests

### Fixed

- None

## 1.2.0
Released Aug 17, 2021

### New

- Update to be compatible with latest version of sudoplatformconfig.json
- Replace android extensions
- Remove jcenter and update some dependencies

### Fixed

- None

## 1.1.0
Released Mar l, 2021.

### New

- Updated Sudo User dependency to 9.2.0

### Fixed

- None

## 1.0.0
Released Jan 18, 2021.

### New

- Initial Release.

### Fixed

- None
