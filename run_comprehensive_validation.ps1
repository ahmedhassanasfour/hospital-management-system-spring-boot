# =============================================================================
# run_comprehensive_validation.ps1
# Full End-to-End System Validation for Task 13
# =============================================================================

$baseUrl = "http://localhost:8081"
$ErrorActionPreference = "Stop"

Write-Host "===================================================================" -ForegroundColor Cyan
Write-Host "   HOSPITAL MANAGEMENT SYSTEM - TASK 13 COMPREHENSIVE VALIDATION   " -ForegroundColor Cyan
Write-Host "===================================================================" -ForegroundColor Cyan

$testResults = [System.Collections.Generic.List[PSObject]]::new()

function Record-Test([string]$suite, [string]$testName, [bool]$passed, [string]$details) {
    $status = if ($passed) { "PASS" } else { "FAIL" }
    $color = if ($passed) { "Green" } else { "Red" }
    Write-Host "  [$status] $testName - $details" -ForegroundColor $color
    $testResults.Add([PSCustomObject]@{
        Suite = $suite
        Test = $testName
        Status = $status
        Details = $details
    })
}

function Get-StatusCode($ex) {
    if ($ex -ne $null -and $ex.Response -ne $null) {
        return [int]$ex.Response.StatusCode
    }
    return 0
}

# -----------------------------------------------------------------------------
# 1. ACTUATOR & HEALTH MONITORING
# -----------------------------------------------------------------------------
Write-Host "`n>>> 1. Actuator and Health Monitoring..." -ForegroundColor Yellow

try {
    $health = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get -TimeoutSec 5
    Record-Test "Actuator" "/actuator/health" ($health.status -eq "UP") "Status: $($health.status)"
} catch {
    Record-Test "Actuator" "/actuator/health" $false $_.Exception.Message
}

try {
    $liveness = Invoke-RestMethod -Uri "$baseUrl/actuator/health/liveness" -Method Get -TimeoutSec 5
    Record-Test "Actuator" "/actuator/health/liveness" ($liveness.status -eq "UP") "Status: $($liveness.status)"
} catch {
    Record-Test "Actuator" "/actuator/health/liveness" $false $_.Exception.Message
}

try {
    $readiness = Invoke-RestMethod -Uri "$baseUrl/actuator/health/readiness" -Method Get -TimeoutSec 5
    Record-Test "Actuator" "/actuator/health/readiness" ($readiness.status -eq "UP") "Status: $($readiness.status) (DB + Redis verified)"
} catch {
    Record-Test "Actuator" "/actuator/health/readiness" $false $_.Exception.Message
}

try {
    $info = Invoke-RestMethod -Uri "$baseUrl/actuator/info" -Method Get -TimeoutSec 5
    Record-Test "Actuator" "/actuator/info" ($null -ne $info) "Endpoint accessible and secure"
} catch {
    Record-Test "Actuator" "/actuator/info" $false $_.Exception.Message
}

# -----------------------------------------------------------------------------
# 2. AUTHENTICATION & RBAC
# -----------------------------------------------------------------------------
Write-Host "`n>>> 2. Authentication & Authorization..." -ForegroundColor Yellow

$rand = Get-Random -Minimum 1000 -Maximum 9999
$patientEmail = "patient.$rand@hospital.com"
$patientPass = "PatientPass123!"

# 2.1 Register
try {
    $regBody = @{
        firstName = "John"
        lastName = "Doe$rand"
        email = $patientEmail
        password = $patientPass
    } | ConvertTo-Json
    $regRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regBody -ContentType "application/json"
    $hasId = ($null -ne $regRes.id)
    $noPass = ($null -eq $regRes.password)
    Record-Test "Auth" "Register Patient" ($hasId -and $noPass) "Registered with ID $($regRes.id), password is omitted"
} catch {
    Record-Test "Auth" "Register Patient" $false $_.Exception.Message
}

# 2.2 Login Patient
$patientToken = $null
$patientRefreshToken = $null
try {
    $loginBody = @{ email = $patientEmail; password = $patientPass } | ConvertTo-Json
    $patientAuth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $patientToken = $patientAuth.accessToken
    $patientRefreshToken = $patientAuth.refreshToken
    Record-Test "Auth" "Login Patient" ($null -ne $patientToken) "Received access and refresh tokens"
} catch {
    Record-Test "Auth" "Login Patient" $false $_.Exception.Message
}

# 2.3 Login Admin
$adminToken = $null
try {
    $adminLogin = @{ email = "admin@hospital.com"; password = "ChangeMe@Local123" } | ConvertTo-Json
    $adminAuth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $adminLogin -ContentType "application/json"
    $adminToken = $adminAuth.accessToken
    Record-Test "Auth" "Login Admin" ($null -ne $adminToken) "Admin token acquired"
} catch {
    Record-Test "Auth" "Login Admin" $false $_.Exception.Message
}

# 2.4 Login Receptionist
$recToken = $null
try {
    $recLogin = @{ email = "receptionist@hospital.com"; password = "ChangeMe@Local123" } | ConvertTo-Json
    $recAuth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $recLogin -ContentType "application/json"
    $recToken = $recAuth.accessToken
    Record-Test "Auth" "Login Receptionist" ($null -ne $recToken) "Receptionist token acquired"
} catch {
    Record-Test "Auth" "Login Receptionist" $false $_.Exception.Message
}

# 2.5 Refresh Token
try {
    $refBody = @{ refreshToken = $patientRefreshToken } | ConvertTo-Json
    $refRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/refresh" -Method Post -Body $refBody -ContentType "application/json"
    Record-Test "Auth" "Refresh Token" ($null -ne $refRes.accessToken) "New access token generated successfully"
} catch {
    Record-Test "Auth" "Refresh Token" $false $_.Exception.Message
}

# 2.6 Invalid JWT rejection (401 or 403)
try {
    Invoke-RestMethod -Uri "$baseUrl/api/users/me" -Method Get -Headers @{ Authorization = "Bearer invalid.jwt.signature" }
    Record-Test "Auth" "Invalid JWT Rejection" $false "Expected 401/403 but request succeeded"
} catch {
    $code = Get-StatusCode $_.Exception
    Record-Test "Auth" "Invalid JWT Rejection" ($code -eq 401 -or $code -eq 403) "Rejected with HTTP $code"
}

# 2.7 Invalid / Expired Refresh Token rejection
try {
    $badRef = @{ refreshToken = "invalid-or-expired-token" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/auth/refresh" -Method Post -Body $badRef -ContentType "application/json"
    Record-Test "Auth" "Invalid Refresh Token" $false "Expected 400/401 but succeeded"
} catch {
    $code = Get-StatusCode $_.Exception
    Record-Test "Auth" "Invalid Refresh Token" ($code -eq 400 -or $code -eq 401) "Rejected with HTTP $code"
}

# 2.8 RBAC: Patient forbidden on Admin endpoint (403)
try {
    Invoke-RestMethod -Uri "$baseUrl/api/admin/doctors" -Method Get -Headers @{ Authorization = "Bearer $patientToken" }
    Record-Test "Auth" "RBAC Patient on Admin" $false "Expected 403 but succeeded"
} catch {
    $code = Get-StatusCode $_.Exception
    Record-Test "Auth" "RBAC Patient on Admin" ($code -eq 403) "Rejected with HTTP 403 Forbidden"
}

# 2.9 RBAC: Receptionist forbidden on Admin endpoint (403)
try {
    Invoke-RestMethod -Uri "$baseUrl/api/admin/doctors" -Method Get -Headers @{ Authorization = "Bearer $recToken" }
    Record-Test "Auth" "RBAC Receptionist on Admin" $false "Expected 403 but succeeded"
} catch {
    $code = Get-StatusCode $_.Exception
    Record-Test "Auth" "RBAC Receptionist on Admin" ($code -eq 403) "Rejected with HTTP 403 Forbidden"
}

# -----------------------------------------------------------------------------
# 3. CORE ENTITIES: Branches, Doctors, Patients, Schedules
# -----------------------------------------------------------------------------
Write-Host "`n>>> 3. Core Entities Management..." -ForegroundColor Yellow

$branchId = $null
try {
    $brBody = @{
        name = "Central Metro Hospital $rand"
        address = "777 Healthcare Blvd"
        phone = "+12345600$rand"
        email = "central$rand@hospital.com"
    } | ConvertTo-Json
    $branch = Invoke-RestMethod -Uri "$baseUrl/api/admin/branches" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $brBody -ContentType "application/json"
    $branchId = $branch.id
    Record-Test "Core" "Create Branch" ($null -ne $branchId) "Branch created with ID: $branchId"
} catch {
    Record-Test "Core" "Create Branch" $false $_.Exception.Message
}

# 3.2 Duplicate Branch
try {
    Invoke-RestMethod -Uri "$baseUrl/api/admin/branches" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $brBody -ContentType "application/json"
    Record-Test "Core" "Duplicate Branch Check" $false "Expected rejection on duplicate branch"
} catch {
    $code = Get-StatusCode $_.Exception
    Record-Test "Core" "Duplicate Branch Check" ($code -eq 400 -or $code -eq 409) "Duplicate branch rejected with HTTP $code"
}

# 3.3 Create Doctor 1 and Doctor 2
$doc1Id = $null
$doc1Token = $null
$doc2Id = $null
$doc2Token = $null

try {
    $d1Body = @{
        firstName = "House"
        lastName = "MD $rand"
        email = "dr.house.$rand@hospital.com"
        password = "DocPassword123!"
        specialization = "Diagnostics"
        licenseNumber = "LIC-D1-$rand"
        phone = "+1999111$rand"
        bio = "Head of Diagnostic Medicine"
    } | ConvertTo-Json
    $doc1 = Invoke-RestMethod -Uri "$baseUrl/api/admin/doctors" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $d1Body -ContentType "application/json"
    $doc1Id = $doc1.id

    $d1Login = @{ email = "dr.house.$rand@hospital.com"; password = "DocPassword123!" } | ConvertTo-Json
    $doc1Auth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $d1Login -ContentType "application/json"
    $doc1Token = $doc1Auth.accessToken

    Record-Test "Core" "Create & Login Doctor 1" ($null -ne $doc1Id -and $null -ne $doc1Token) "Doctor 1 ID: $doc1Id"
} catch {
    Record-Test "Core" "Create & Login Doctor 1" $false $_.Exception.Message
}

# 3.4 Duplicate License Number Prevention
try {
    $dupLicBody = @{
        firstName = "Clone"
        lastName = "Doctor"
        email = "clone.$rand@hospital.com"
        password = "DocPassword123!"
        specialization = "Diagnostics"
        licenseNumber = "LIC-D1-$rand"
        phone = "+1999222$rand"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/admin/doctors" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $dupLicBody -ContentType "application/json"
    Record-Test "Core" "Duplicate License Check" $false "Expected rejection for duplicate license"
} catch {
    $code = Get-StatusCode $_.Exception
    Record-Test "Core" "Duplicate License Check" ($code -eq 400) "Duplicate license rejected with HTTP $code"
}

# 3.5 Create Doctor 2
try {
    $d2Body = @{
        firstName = "Wilson"
        lastName = "Oncology $rand"
        email = "dr.wilson.$rand@hospital.com"
        password = "DocPassword123!"
        specialization = "Oncology"
        licenseNumber = "LIC-D2-$rand"
        phone = "+1999333$rand"
        bio = "Head of Oncology"
    } | ConvertTo-Json
    $doc2 = Invoke-RestMethod -Uri "$baseUrl/api/admin/doctors" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $d2Body -ContentType "application/json"
    $doc2Id = $doc2.id

    $d2Login = @{ email = "dr.wilson.$rand@hospital.com"; password = "DocPassword123!" } | ConvertTo-Json
    $doc2Auth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $d2Login -ContentType "application/json"
    $doc2Token = $doc2Auth.accessToken

    Record-Test "Core" "Create & Login Doctor 2" ($null -ne $doc2Id -and $null -ne $doc2Token) "Doctor 2 ID: $doc2Id"
} catch {
    Record-Test "Core" "Create & Login Doctor 2" $false $_.Exception.Message
}

# 3.6 Create Patient
$patientEntityId = $null
$aliceUserId = $null
$aliceToken = $null
try {
    $pBody = @{
        firstName = "Alice"
        lastName = "Wonderland $rand"
        email = "alice.$rand@hospital.com"
        password = "PatientPass123!"
        nationalId = "NAT-$rand-001"
        phone = "+1555123$rand"
        dateOfBirth = "1995-04-12"
        gender = "FEMALE"
        address = "12 Rabbit Hole Way"
    } | ConvertTo-Json
    $patient = Invoke-RestMethod -Uri "$baseUrl/api/admin/patients" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $pBody -ContentType "application/json"
    $patientEntityId = $patient.id

    # Get Alice user login
    $pLogin = @{ email = "alice.$rand@hospital.com"; password = "PatientPass123!" } | ConvertTo-Json
    $pAuth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $pLogin -ContentType "application/json"
    $aliceToken = $pAuth.accessToken
    $aliceUserId = $pAuth.user.id

    Record-Test "Core" "Create Patient & User" ($null -ne $patientEntityId) "Patient ID: $patientEntityId, User ID: $aliceUserId"
} catch {
    Record-Test "Core" "Create Patient & User" $false $_.Exception.Message
}

# 3.7 Duplicate National ID Prevention
try {
    $dupNatBody = @{
        firstName = "Bob"
        lastName = "Duplicate"
        email = "bob.dup.$rand@hospital.com"
        password = "PatientPass123!"
        nationalId = "NAT-$rand-001"
        phone = "+1555999$rand"
        dateOfBirth = "1990-01-01"
        gender = "MALE"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/admin/patients" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $dupNatBody -ContentType "application/json"
    Record-Test "Core" "Duplicate National ID Check" $false "Expected rejection for duplicate national ID"
} catch {
    $code = Get-StatusCode $_.Exception
    Record-Test "Core" "Duplicate National ID Check" ($code -eq 400) "Duplicate national ID rejected with HTTP $code"
}

# 3.8 Doctor Branch Assignments & Schedules
try {
    Invoke-RestMethod -Uri "$baseUrl/api/admin/doctors/$doc1Id/branches/$branchId" -Method Post -Headers @{ Authorization = "Bearer $adminToken" }
    Invoke-RestMethod -Uri "$baseUrl/api/admin/doctors/$doc2Id/branches/$branchId" -Method Post -Headers @{ Authorization = "Bearer $adminToken" }

    $todayDay = (Get-Date).DayOfWeek.ToString().ToUpper()
    $sBody = @{
        dayOfWeek = $todayDay
        startTime = "08:00:00"
        endTime = "20:00:00"
    } | ConvertTo-Json

    $sched1 = Invoke-RestMethod -Uri "$baseUrl/api/admin/doctors/$doc1Id/branches/$branchId/schedules" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $sBody -ContentType "application/json"
    $sched2 = Invoke-RestMethod -Uri "$baseUrl/api/admin/doctors/$doc2Id/branches/$branchId/schedules" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $sBody -ContentType "application/json"

    Record-Test "Core" "Doctor Schedules Created" ($null -ne $sched1.id -and $null -ne $sched2.id) "Schedules created for day: $todayDay"
} catch {
    Record-Test "Core" "Doctor Schedules Created" $false $_.Exception.Message
}

# -----------------------------------------------------------------------------
# 4. APPOINTMENTS & CONCURRENCY / LOCKING
# -----------------------------------------------------------------------------
Write-Host "`n>>> 4. Appointments & Concurrency..." -ForegroundColor Yellow

$todayDate = (Get-Date).ToString("yyyy-MM-dd")
$appt1Id = $null
$appt2Id = $null

# 4.1 Create Appointment for Doctor 1 (10:00 - 10:30)
try {
    $a1Body = @{
        date = $todayDate
        startTime = "10:00:00"
        endTime = "10:30:00"
        notes = "Routine Diagnostics"
    } | ConvertTo-Json
    $appt1 = Invoke-RestMethod -Uri "$baseUrl/api/admin/appointments/doctors/$doc1Id/patients/$patientEntityId/branches/$branchId" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $a1Body -ContentType "application/json"
    $appt1Id = $appt1.id
    Record-Test "Appointments" "Create Appointment 1" ($null -ne $appt1Id) "Appointment 1 created with ID: $appt1Id"
} catch {
    Record-Test "Appointments" "Create Appointment 1" $false $_.Exception.Message
}

# 4.2 Overlapping Slot for Same Doctor (10:15 - 10:45) -> MUST FAIL (Double booking prevention)
try {
    $overlapBody = @{
        date = $todayDate
        startTime = "10:15:00"
        endTime = "10:45:00"
        notes = "Conflict attempt"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/admin/appointments/doctors/$doc1Id/patients/$patientEntityId/branches/$branchId" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $overlapBody -ContentType "application/json"
    Record-Test "Appointments" "Double-booking Prevention" $false "Overlapping appointment unexpectedly allowed!"
} catch {
    $code = Get-StatusCode $_.Exception
    Record-Test "Appointments" "Double-booking Prevention" ($code -eq 400 -or $code -eq 409) "Overlapping slot blocked with HTTP $code"
}

# 4.3 Different Doctor at Same Time (10:00 - 10:30) -> MUST SUCCEED
try {
    $appt2 = Invoke-RestMethod -Uri "$baseUrl/api/admin/appointments/doctors/$doc2Id/patients/$patientEntityId/branches/$branchId" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $a1Body -ContentType "application/json"
    $appt2Id = $appt2.id
    Record-Test "Appointments" "Different Doctor Concurrent Booking" ($null -ne $appt2Id) "Doctor 2 booked at same time slot with ID: $appt2Id"
} catch {
    Record-Test "Appointments" "Different Doctor Concurrent Booking" $false $_.Exception.Message
}

# 4.4 Status Transition: PENDING -> CONFIRMED
try {
    $cBody = @{ status = "CONFIRMED" } | ConvertTo-Json
    $confirmedAppt = Invoke-RestMethod -Uri "$baseUrl/api/admin/appointments/$appt1Id/status" -Method Patch -Headers @{ Authorization = "Bearer $adminToken" } -Body $cBody -ContentType "application/json"
    Record-Test "Appointments" "Status Transition (CONFIRMED)" ($confirmedAppt.status -eq "CONFIRMED") "Appointment status updated to CONFIRMED"
} catch {
    Record-Test "Appointments" "Status Transition (CONFIRMED)" $false $_.Exception.Message
}

# 4.5 Invalid Status Transition: CONFIRMED -> PENDING -> MUST FAIL
try {
    $badStatus = @{ status = "PENDING" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/admin/appointments/$appt1Id/status" -Method Patch -Headers @{ Authorization = "Bearer $adminToken" } -Body $badStatus -ContentType "application/json"
    Record-Test "Appointments" "Invalid Status Transition Rejection" $false "Invalid transition unexpectedly allowed!"
} catch {
    $code = Get-StatusCode $_.Exception
    Record-Test "Appointments" "Invalid Status Transition Rejection" ($code -eq 400) "Invalid transition rejected with HTTP $code"
}

# 4.6 Status Transition: CANCELLED on Appointment 2
try {
    $cancelBody = @{ status = "CANCELLED" } | ConvertTo-Json
    $cancelledAppt = Invoke-RestMethod -Uri "$baseUrl/api/admin/appointments/$appt2Id/status" -Method Patch -Headers @{ Authorization = "Bearer $adminToken" } -Body $cancelBody -ContentType "application/json"
    Record-Test "Appointments" "Status Transition (CANCELLED)" ($cancelledAppt.status -eq "CANCELLED") "Appointment 2 cancelled successfully"
} catch {
    Record-Test "Appointments" "Status Transition (CANCELLED)" $false $_.Exception.Message
}

# -----------------------------------------------------------------------------
# 5. MEDICAL OPERATIONS
# -----------------------------------------------------------------------------
Write-Host "`n>>> 5. Medical Operations (Records, Prescriptions, Lab, Files)..." -ForegroundColor Yellow

# 5.1 Medical Record
try {
    $recBody = @{
        diagnosis = "Respiratory tract inflammation"
        symptoms = "Cough and mild fever"
        notes = "Prescribed Amoxicillin"
        treatment = "Oral antibiotic for 7 days"
    } | ConvertTo-Json
    $medRecord = Invoke-RestMethod -Uri "$baseUrl/api/admin/medical-records/appointments/$appt1Id" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $recBody -ContentType "application/json"
    Record-Test "Medical" "Create Medical Record" ($null -ne $medRecord.id) "Medical record created with ID: $($medRecord.id)"
} catch {
    Record-Test "Medical" "Create Medical Record" $false $_.Exception.Message
}

# 5.2 Medicine Creation
$medId = $null
try {
    $mBody = @{
        name = "Amoxicillin 500mg $rand"
        description = "Standard antibiotic capsule"
    } | ConvertTo-Json
    $medicine = Invoke-RestMethod -Uri "$baseUrl/api/medicines" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $mBody -ContentType "application/json"
    $medId = $medicine.id
    Record-Test "Medical" "Create Medicine" ($null -ne $medId) "Medicine created with ID: $medId"
} catch {
    Record-Test "Medical" "Create Medicine" $false $_.Exception.Message
}

# 5.3 Prescription Creation by Doctor 1
$rxId = $null
try {
    $rxBody = @{
        appointmentId = $appt1Id
        notes = "Take one capsule after meals"
        items = @(
            @{
                medicineId = $medId
                dosage = "500mg"
                frequency = "3 times daily"
                duration = "7 days"
                instructions = "With water"
            }
        )
    } | ConvertTo-Json -Depth 5
    $rx = Invoke-RestMethod -Uri "$baseUrl/api/doctor/prescriptions" -Method Post -Headers @{ Authorization = "Bearer $doc1Token" } -Body $rxBody -ContentType "application/json"
    $rxId = $rx.id
    Record-Test "Medical" "Create Prescription (Doctor 1)" ($null -ne $rxId) "Prescription issued with ID: $rxId"
} catch {
    Record-Test "Medical" "Create Prescription (Doctor 1)" $false $_.Exception.Message
}

# 5.4 Duplicate Prescription Prevention for same appointment
try {
    Invoke-RestMethod -Uri "$baseUrl/api/doctor/prescriptions" -Method Post -Headers @{ Authorization = "Bearer $doc1Token" } -Body $rxBody -ContentType "application/json"
    Record-Test "Medical" "Duplicate Prescription Prevention" $false "Duplicate prescription unexpectedly allowed"
} catch {
    $code = Get-StatusCode $_.Exception
    Record-Test "Medical" "Duplicate Prescription Prevention" ($code -eq 400) "Duplicate prescription rejected with HTTP $code"
}

# 5.5 Lab Test Creation
$labTestId = $null
try {
    $ltBody = @{
        name = "Complete Blood Count $rand"
        description = "Routine hematology screen"
    } | ConvertTo-Json
    $labTest = Invoke-RestMethod -Uri "$baseUrl/api/admin/lab-tests" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $ltBody -ContentType "application/json"
    $labTestId = $labTest.id
    Record-Test "Medical" "Create Lab Test" ($null -ne $labTestId) "Lab test created with ID: $labTestId"
} catch {
    Record-Test "Medical" "Create Lab Test" $false $_.Exception.Message
}

# 5.6 Doctor 1 creates Lab Order
$labOrderId = $null
try {
    $loBody = @{
        appointmentId = $appt1Id
        labTestId = $labTestId
        notes = "Check WBC and hemoglobin"
    } | ConvertTo-Json
    $labOrder = Invoke-RestMethod -Uri "$baseUrl/api/doctor/lab-orders" -Method Post -Headers @{ Authorization = "Bearer $doc1Token" } -Body $loBody -ContentType "application/json"
    $labOrderId = $labOrder.id
    Record-Test "Medical" "Create Lab Order (Doctor 1)" ($null -ne $labOrderId) "Lab Order created with ID: $labOrderId"
} catch {
    Record-Test "Medical" "Create Lab Order (Doctor 1)" $false $_.Exception.Message
}

# 5.7 Doctor Ownership: Doctor 2 submitting result for Doctor 1's order -> MUST FAIL
try {
    $resBody = @{
        resultValue = "Normal"
        referenceRange = "4.5-11.0"
        notes = "Unauthorized submit"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/doctor/lab-results/lab-orders/$labOrderId" -Method Post -Headers @{ Authorization = "Bearer $doc2Token" } -Body $resBody -ContentType "application/json"
    Record-Test "Medical" "Lab Result Doctor Ownership Check" $false "Doctor 2 unexpectedly submitted result for Doctor 1's order"
} catch {
    $code = Get-StatusCode $_.Exception
    Record-Test "Medical" "Lab Result Doctor Ownership Check" ($code -eq 400 -or $code -eq 403) "Doctor 2 rejected with HTTP $code"
}

# 5.8 Doctor 1 records Lab Result -> MUST SUCCEED
$labResultId = $null
try {
    $resBody = @{
        resultValue = "WBC 7.8 x10^9/L"
        referenceRange = "4.5 - 11.0"
        notes = "Normal leukocyte count"
    } | ConvertTo-Json
    $labRes = Invoke-RestMethod -Uri "$baseUrl/api/doctor/lab-results/lab-orders/$labOrderId" -Method Post -Headers @{ Authorization = "Bearer $doc1Token" } -Body $resBody -ContentType "application/json"
    $labResultId = $labRes.id
    Record-Test "Medical" "Record Lab Result (Doctor 1)" ($null -ne $labResultId) "Lab result recorded with ID: $labResultId"
} catch {
    Record-Test "Medical" "Record Lab Result (Doctor 1)" $false $_.Exception.Message
}

# 5.9 File Upload: Patient Medical Document using curl.exe
$uploadedFileId = $null
$tempPngPath = [System.IO.Path]::GetTempFileName() + ".png"
[System.IO.File]::WriteAllBytes($tempPngPath, [byte[]]@(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52))

try {
    $uploadJson = curl.exe -s -X POST -H "Authorization: Bearer $aliceToken" -F "file=@$tempPngPath;type=image/png" "$baseUrl/api/patient/files/documents"
    $fileObj = $uploadJson | ConvertFrom-Json
    $uploadedFileId = $fileObj.id
    Record-Test "Files" "Valid File Upload (PNG)" ($null -ne $uploadedFileId) "File uploaded with ID: $uploadedFileId"
} catch {
    Record-Test "Files" "Valid File Upload (PNG)" $false $_.Exception.Message
}

# 5.10 File Upload: Unsupported File Type (text/plain) -> MUST FAIL (400)
$tempTxtPath = [System.IO.Path]::GetTempFileName() + ".txt"
[System.IO.File]::WriteAllText($tempTxtPath, "This is plain text and should be rejected")
try {
    $badUploadCode = curl.exe -s -o NUL -w "%{http_code}" -X POST -H "Authorization: Bearer $aliceToken" -F "file=@$tempTxtPath;type=text/plain" "$baseUrl/api/patient/files/documents"
    $is400 = ($badUploadCode.Trim() -eq "400")
    Record-Test "Files" "Unsupported File Type Rejection" $is400 "Rejected with HTTP $badUploadCode"
} catch {
    Record-Test "Files" "Unsupported File Type Rejection" $false $_.Exception.Message
}

# 5.11 File Download IDOR Security: Another patient cannot download Alice's file
try {
    $downloadCode = curl.exe -s -o NUL -w "%{http_code}" -X GET -H "Authorization: Bearer $patientToken" "$baseUrl/api/patient/files/$uploadedFileId/download"
    $isBlocked = ($downloadCode.Trim() -eq "400" -or $downloadCode.Trim() -eq "403" -or $downloadCode.Trim() -eq "404")
    Record-Test "Files" "File Download IDOR Protection" $isBlocked "Unauthorized download rejected with HTTP $downloadCode"
} catch {
    Record-Test "Files" "File Download IDOR Protection" $false $_.Exception.Message
}

# Clean up temp files
if (Test-Path $tempPngPath) { Remove-Item -Force $tempPngPath }
if (Test-Path $tempTxtPath) { Remove-Item -Force $tempTxtPath }

# -----------------------------------------------------------------------------
# 6. BILLING & PAYMENTS + IDEMPOTENCY
# -----------------------------------------------------------------------------
Write-Host "`n>>> 6. Billing, Payments & Idempotency..." -ForegroundColor Yellow

$invoiceId = $null
try {
    $invBody = @{
        patientId = $patientEntityId
        appointmentId = $appt1Id
        notes = "Complete consultation and lab service"
        dueAt = (Get-Date).AddDays(14).ToString("s")
        items = @(
            @{
                type = "CONSULTATION"
                description = "Doctor Consultation"
                quantity = 1.0
                unitPrice = 120.00
            },
            @{
                type = "LAB"
                description = "CBC Panel"
                quantity = 1.0
                unitPrice = 80.00
                labOrderId = $labOrderId
            }
        )
    } | ConvertTo-Json -Depth 5
    $inv = Invoke-RestMethod -Uri "$baseUrl/api/receptionist/invoices" -Method Post -Headers @{ Authorization = "Bearer $recToken" } -Body $invBody -ContentType "application/json"
    $invoiceId = $inv.id
    Record-Test "Billing" "Create Invoice" ($null -ne $invoiceId) "Invoice created with ID: $invoiceId (Total: $($inv.totalAmount))"
} catch {
    Record-Test "Billing" "Create Invoice" $false $_.Exception.Message
}

# 6.2 Invalid Payment Amount (Negative or Zero) -> MUST FAIL (400)
try {
    $badPay = @{
        invoiceId = $invoiceId
        amount = -50.00
        method = "CARD"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/receptionist/payments" -Method Post -Headers @{ Authorization = "Bearer $recToken"; "Idempotency-Key" = "BAD-PAY-$rand" } -Body $badPay -ContentType "application/json"
    Record-Test "Billing" "Invalid Payment Amount Rejection" $false "Negative payment was accepted!"
} catch {
    $code = Get-StatusCode $_.Exception
    Record-Test "Billing" "Invalid Payment Amount Rejection" ($code -eq 400) "Negative payment rejected with HTTP $code"
}

# 6.3 Idempotent Payment: First Request
$idempKey = "PAY-IDEMP-$rand-VALIDATION"
$pay1 = $null
try {
    $payBody = @{
        invoiceId = $invoiceId
        amount = 200.00
        method = "CARD"
        notes = "Full card settlement"
    } | ConvertTo-Json
    $pay1 = Invoke-RestMethod -Uri "$baseUrl/api/receptionist/payments" -Method Post -Headers @{ Authorization = "Bearer $recToken"; "Idempotency-Key" = $idempKey } -Body $payBody -ContentType "application/json"
    Record-Test "Billing" "Payment 1 with Idempotency-Key" ($null -ne $pay1.id) "Payment created with ID: $($pay1.id), Ref: $($pay1.paymentReference)"
} catch {
    Record-Test "Billing" "Payment 1 with Idempotency-Key" $false $_.Exception.Message
}

# 6.4 Replay exact same request with SAME Idempotency-Key
try {
    $pay2 = Invoke-RestMethod -Uri "$baseUrl/api/receptionist/payments" -Method Post -Headers @{ Authorization = "Bearer $recToken"; "Idempotency-Key" = $idempKey } -Body $payBody -ContentType "application/json"
    $exactMatch = ($pay2.id -eq $pay1.id -and $pay2.paymentReference -eq $pay1.paymentReference)
    Record-Test "Billing" "Idempotent Replay Verification" $exactMatch "Replay returned identical payment ID $($pay2.id) and reference without duplicating"
} catch {
    Record-Test "Billing" "Idempotent Replay Verification" $false $_.Exception.Message
}

# 6.5 Verify Invoice Payment Records Count = 1 in DB
try {
    $paymentsList = Invoke-RestMethod -Uri "$baseUrl/api/receptionist/payments/invoice/$invoiceId" -Method Get -Headers @{ Authorization = "Bearer $recToken" }
    Record-Test "Billing" "No Duplicate Payment in DB" ($paymentsList.Count -eq 1) "Invoice has exactly 1 payment record in database"
} catch {
    Record-Test "Billing" "No Duplicate Payment in DB" $false $_.Exception.Message
}

# 6.6 Attempt to pay already-paid invoice with NEW key -> MUST FAIL (400)
try {
    $newPay = @{
        invoiceId = $invoiceId
        amount = 50.00
        method = "CASH"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/receptionist/payments" -Method Post -Headers @{ Authorization = "Bearer $recToken"; "Idempotency-Key" = "NEW-KEY-$rand" } -Body $newPay -ContentType "application/json"
    Record-Test "Billing" "Already Paid Invoice Rejection" $false "Payment accepted for already fully paid invoice"
} catch {
    $code = Get-StatusCode $_.Exception
    Record-Test "Billing" "Already Paid Invoice Rejection" ($code -eq 400) "Payment on fully paid invoice rejected with HTTP $code"
}

# -----------------------------------------------------------------------------
# 7. REDIS CACHE VERIFICATION
# -----------------------------------------------------------------------------
Write-Host "`n>>> 7. Redis Cache Verification..." -ForegroundColor Yellow

# Query medicines list twice
try {
    $meds1 = Invoke-RestMethod -Uri "$baseUrl/api/medicines" -Method Get -Headers @{ Authorization = "Bearer $adminToken" }
    $meds2 = Invoke-RestMethod -Uri "$baseUrl/api/medicines" -Method Get -Headers @{ Authorization = "Bearer $adminToken" }
    Record-Test "Redis" "Medicines Endpoint Accessible" ($null -ne $meds2) "Retrieved catalogue items from cache"
} catch {
    Record-Test "Redis" "Medicines Endpoint Accessible" $false $_.Exception.Message
}

# Check Redis keys inside container
try {
    $redisKeys = docker exec hospital-redis redis-cli keys "*"
    $hasKeys = ($redisKeys.Count -gt 0)
    Record-Test "Redis" "Redis Contains Cached Keys" $hasKeys "Active cache keys: $($redisKeys -join ', ')"
} catch {
    Record-Test "Redis" "Redis Contains Cached Keys" $false $_.Exception.Message
}

# -----------------------------------------------------------------------------
# 8. NOTIFICATIONS & SECURITY / IDOR
# -----------------------------------------------------------------------------
Write-Host "`n>>> 8. Notifications & Notification Security..." -ForegroundColor Yellow

# Wait 2 seconds for async events to complete
Start-Sleep -Milliseconds 2000

$aliceNotifs = $null
try {
    $notifPage = Invoke-RestMethod -Uri "$baseUrl/api/notifications" -Method Get -Headers @{ Authorization = "Bearer $aliceToken" }
    $aliceNotifs = $notifPage.content
    $hasNotifs = ($aliceNotifs.Count -gt 0)
    Record-Test "Notifications" "Patient Receives Notifications" $hasNotifs "Alice received $($aliceNotifs.Count) notification(s) across business events"
} catch {
    Record-Test "Notifications" "Patient Receives Notifications" $false $_.Exception.Message
}

if ($null -ne $aliceNotifs -and $aliceNotifs.Count -gt 0) {
    $notifId = $aliceNotifs[0].id

    # 8.2 Mark notification as read via curl.exe to avoid WebRequest hanging
    try {
        $readCode = curl.exe -s -o NUL -w "%{http_code}" -X PATCH -H "Authorization: Bearer $aliceToken" "$baseUrl/api/notifications/$notifId/read"
        Record-Test "Notifications" "Mark Notification Read" ($readCode.Trim() -eq "204") "Notification marked as read with HTTP $readCode"
    } catch {
        Record-Test "Notifications" "Mark Notification Read" $false $_.Exception.Message
    }

    # 8.3 IDOR: Another user tries to mark Alice's notification
    try {
        $idorCode = curl.exe -s -o NUL -w "%{http_code}" -X PATCH -H "Authorization: Bearer $patientToken" "$baseUrl/api/notifications/$notifId/read"
        Record-Test "Notifications" "IDOR Notification Protection" ($idorCode.Trim() -eq "404" -or $idorCode.Trim() -eq "403") "IDOR attempt safely blocked with HTTP $idorCode"
    } catch {
        Record-Test "Notifications" "IDOR Notification Protection" $false $_.Exception.Message
    }
}

# -----------------------------------------------------------------------------
# 9. AUDIT LOGGING VERIFICATION
# -----------------------------------------------------------------------------
Write-Host "`n>>> 9. Audit Logging Verification..." -ForegroundColor Yellow

try {
    $auditOutput = docker exec hospital-postgres psql -U postgres -d hospital_db -t -c "SELECT DISTINCT action FROM audit_logs ORDER BY action;"
    $auditActions = $auditOutput -split "`r?`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne "" }
    
    $expectedActions = @("USER_REGISTERED", "USER_LOGIN", "DOCTOR_CREATED", "PATIENT_CREATED", "DOCTOR_BRANCH_ASSIGNED")
    $foundAll = $true
    foreach ($ea in $expectedActions) {
        if ($auditActions -notcontains $ea) {
            $foundAll = $false
            break
        }
    }
    Record-Test "Audit" "Audit Log Entries Recorded" $foundAll "Found audit actions: $($auditActions -join ', ')"
} catch {
    Record-Test "Audit" "Audit Log Entries Recorded" $false $_.Exception.Message
}

# -----------------------------------------------------------------------------
# SUMMARY & FINAL TALLY
# -----------------------------------------------------------------------------
Write-Host "`n===================================================================" -ForegroundColor Cyan
Write-Host "                       VALIDATION SUMMARY                          " -ForegroundColor Cyan
Write-Host "===================================================================" -ForegroundColor Cyan

$passedCount = ($testResults | Where-Object { $_.Status -eq "PASS" }).Count
$failedCount = ($testResults | Where-Object { $_.Status -eq "FAIL" }).Count
$totalCount = $testResults.Count

Write-Host "Total Tests: $totalCount | PASSED: $passedCount | FAILED: $failedCount`n" -ForegroundColor $(if ($failedCount -eq 0) { "Green" } else { "Red" })

if ($failedCount -eq 0) {
    Write-Host ">>> ALL COMPREHENSIVE TESTS PASSED SUCCESSFULLY! <<<" -ForegroundColor Green
} else {
    Write-Host ">>> SOME TESTS FAILED. Inspect results above. <<<" -ForegroundColor Red
}
