# =============================================================================
# comprehensive_test.ps1 — Full End-to-End System Validation
# =============================================================================

$baseUrl = "http://localhost:8081"
$ErrorActionPreference = "Continue"

Write-Host "=== HOSPITAL MANAGEMENT SYSTEM - COMPREHENSIVE TEST SUITE ===" -ForegroundColor Cyan

function Assert-Equal($actual, $expected, [string]$message) {
    if ($actual -eq $expected) {
        Write-Host "  [PASS] $message" -ForegroundColor Green
        return $true
    } else {
        Write-Host "  [FAIL] $message - Expected: '$expected', Got: '$actual'" -ForegroundColor Red
        return $false
    }
}

function Assert-True([bool]$condition, [string]$message) {
    if ($condition) {
        Write-Host "  [PASS] $message" -ForegroundColor Green
        return $true
    } else {
        Write-Host "  [FAIL] $message" -ForegroundColor Red
        return $false
    }
}

# -----------------------------------------------------------------------------
# 1. Wait for Actuator Readiness
# -----------------------------------------------------------------------------
Write-Host "`n>>> 1. Checking Actuator Readiness..." -ForegroundColor Yellow
$maxRetries = 30
$ready = $false
for ($i = 1; $i -le $maxRetries; $i++) {
    try {
        $res = Invoke-RestMethod -Uri "$baseUrl/actuator/health/readiness" -Method Get -TimeoutSec 3
        if ($res.status -eq "UP") {
            $ready = $true
            break
        }
    } catch {
        Start-Sleep -Seconds 2
    }
}
Assert-True $ready "Application Readiness is UP"

# -----------------------------------------------------------------------------
# 2. Actuator Endpoints Verification
# -----------------------------------------------------------------------------
Write-Host "`n>>> 2. Actuator Endpoints..." -ForegroundColor Yellow
$health = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get
Assert-Equal $health.status "UP" "Root /actuator/health is UP"

$liveness = Invoke-RestMethod -Uri "$baseUrl/actuator/health/liveness" -Method Get
Assert-Equal $liveness.status "UP" "Liveness probe is UP"

$readiness = Invoke-RestMethod -Uri "$baseUrl/actuator/health/readiness" -Method Get
Assert-Equal $readiness.status "UP" "Readiness probe is UP"

$info = Invoke-RestMethod -Uri "$baseUrl/actuator/info" -Method Get
Assert-Equal $info.app.name "Hospital Management System" "/actuator/info returns application name"

# -----------------------------------------------------------------------------
# 3. Authentication and Authorization
# -----------------------------------------------------------------------------
Write-Host "`n>>> 3. Authentication and Authorization..." -ForegroundColor Yellow

# 3.1 Register Patient
$rand = Get-Random -Minimum 1000 -Maximum 9999
$patientEmail = "patient.$rand@hospital.com"
$regBody = @{
    firstName = "Test"
    lastName = "Patient$rand"
    email = $patientEmail
    password = "PatientPassword123!"
} | ConvertTo-Json

$regRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regBody -ContentType "application/json"
Assert-True ($null -ne $regRes.id) "Patient registered successfully"
Assert-True ($null -eq $regRes.password) "Password is NOT exposed in register response"

# 3.2 Login with Patient
$loginPatientBody = @{
    email = $patientEmail
    password = "PatientPassword123!"
} | ConvertTo-Json
$patientAuth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginPatientBody -ContentType "application/json"
$patientToken = $patientAuth.accessToken
Assert-True ($null -ne $patientToken) "Patient logged in, received access token"

# 3.3 Login with Admin
$loginAdminBody = @{
    email = "admin@hospital.com"
    password = "ChangeMe@Local123"
} | ConvertTo-Json
$adminAuth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginAdminBody -ContentType "application/json"
$adminToken = $adminAuth.accessToken
Assert-True ($null -ne $adminToken) "Admin logged in, received access token"

# 3.4 Login with Receptionist
$loginRecBody = @{
    email = "receptionist@hospital.com"
    password = "ChangeMe@Local123"
} | ConvertTo-Json
$recAuth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginRecBody -ContentType "application/json"
$recToken = $recAuth.accessToken
Assert-True ($null -ne $recToken) "Receptionist logged in, received access token"

# 3.5 Token Refresh
$refreshBody = @{
    refreshToken = $patientAuth.refreshToken
} | ConvertTo-Json
$refreshRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/refresh" -Method Post -Body $refreshBody -ContentType "application/json"
Assert-True ($null -ne $refreshRes.accessToken) "Refresh token returned new access token"

# 3.6 Invalid Token (401)
$invalidAuthFailed = $false
try {
    Invoke-RestMethod -Uri "$baseUrl/api/users/me" -Method Get -Headers @{ Authorization = "Bearer invalid.jwt.token" }
} catch {
    $invalidAuthFailed = ($_.Exception.Response.StatusCode.value__ -eq 401)
}
Assert-True $invalidAuthFailed "Invalid JWT rejected with 401 Unauthorized"

# 3.7 Role-based Access Control (RBAC): Patient cannot access Admin endpoints (403)
$rbacBlocked = $false
try {
    Invoke-RestMethod -Uri "$baseUrl/api/admin/doctors" -Method Get -Headers @{ Authorization = "Bearer $patientToken" }
} catch {
    $rbacBlocked = ($_.Exception.Response.StatusCode.value__ -eq 403)
}
Assert-True $rbacBlocked "Patient accessing /api/admin/doctors rejected with 403 Forbidden"

# -----------------------------------------------------------------------------
# 4. Core Entities: Branches, Doctors, Patients, Schedules
# -----------------------------------------------------------------------------
Write-Host "`n>>> 4. Core Entities..." -ForegroundColor Yellow

# 4.1 Create Branch
$branchBody = @{
    name = "City General Branch $rand"
    address = "100 Medical Plaza"
    phone = "+123456789"
    email = "city.branch$rand@hospital.com"
} | ConvertTo-Json
$branch = Invoke-RestMethod -Uri "$baseUrl/api/admin/branches" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $branchBody -ContentType "application/json"
$branchId = $branch.id
Assert-True ($null -ne $branchId) "Branch created with ID: $branchId"

# 4.2 Duplicate Branch Name prevention
$dupBranchBlocked = $false
try {
    Invoke-RestMethod -Uri "$baseUrl/api/admin/branches" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $branchBody -ContentType "application/json"
} catch {
    $dupBranchBlocked = $true
}
Assert-True $dupBranchBlocked "Duplicate branch creation prevented"

# 4.3 Create Doctor 1
$doc1Body = @{
    firstName = "Gregory"
    lastName = "House"
    email = "dr.house$rand@hospital.com"
    password = "DoctorPassword123!"
    specialization = "Diagnostics"
    licenseNumber = "LIC-HOUSE-$rand"
    phone = "+1987654321"
    bio = "Chief of Diagnostics"
} | ConvertTo-Json
$doc1 = Invoke-RestMethod -Uri "$baseUrl/api/admin/doctors" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $doc1Body -ContentType "application/json"
$doc1Id = $doc1.id
Assert-True ($null -ne $doc1Id) "Doctor 1 created with ID: $doc1Id"

# 4.4 Duplicate License Number prevention
$dupLicBlocked = $false
try {
    $dupDocBody = @{
        firstName = "Greg"
        lastName = "Duplicate"
        email = "dr.dup$rand@hospital.com"
        password = "DoctorPassword123!"
        specialization = "Diagnostics"
        licenseNumber = "LIC-HOUSE-$rand"
        phone = "+1987654329"
        bio = "Duplicate"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/admin/doctors" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $dupDocBody -ContentType "application/json"
} catch {
    $dupLicBlocked = ($_.Exception.Response.StatusCode.value__ -eq 400)
}
Assert-True $dupLicBlocked "Duplicate doctor license number prevented with 400 Bad Request"

# 4.5 Create Doctor 2
$doc2Body = @{
    firstName = "James"
    lastName = "Wilson"
    email = "dr.wilson$rand@hospital.com"
    password = "DoctorPassword123!"
    specialization = "Oncology"
    licenseNumber = "LIC-WILSON-$rand"
    phone = "+1987654322"
    bio = "Head of Oncology"
} | ConvertTo-Json
$doc2 = Invoke-RestMethod -Uri "$baseUrl/api/admin/doctors" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $doc2Body -ContentType "application/json"
$doc2Id = $doc2.id
Assert-True ($null -ne $doc2Id) "Doctor 2 created with ID: $doc2Id"

# Doctor 1 Login
$doc1LoginBody = @{ email = "dr.house$rand@hospital.com"; password = "DoctorPassword123!" } | ConvertTo-Json
$doc1Auth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $doc1LoginBody -ContentType "application/json"
$doc1Token = $doc1Auth.accessToken

# Doctor 2 Login
$doc2LoginBody = @{ email = "dr.wilson$rand@hospital.com"; password = "DoctorPassword123!" } | ConvertTo-Json
$doc2Auth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $doc2LoginBody -ContentType "application/json"
$doc2Token = $doc2Auth.accessToken

# 4.6 Create Patient
$patBody = @{
    firstName = "Alice"
    lastName = "Smith"
    email = "alice.smith$rand@hospital.com"
    password = "PatientPassword123!"
    nationalId = "NAT-ID-$rand"
    phone = "+1122334455"
    dateOfBirth = "1990-05-15"
    gender = "FEMALE"
    address = "456 Maple St"
} | ConvertTo-Json
$patient = Invoke-RestMethod -Uri "$baseUrl/api/admin/patients" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $patBody -ContentType "application/json"
$patientId = $patient.id
Assert-True ($null -ne $patientId) "Patient created with ID: $patientId"

# 4.7 Duplicate National ID prevention
$dupNatBlocked = $false
try {
    $dupPatBody = @{
        firstName = "Bob"
        lastName = "Duplicate"
        email = "bob.dup$rand@hospital.com"
        password = "PatientPassword123!"
        nationalId = "NAT-ID-$rand"
        phone = "+1122334499"
        dateOfBirth = "1992-01-01"
        gender = "MALE"
        address = "789 Pine St"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/admin/patients" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $dupPatBody -ContentType "application/json"
} catch {
    $dupNatBlocked = ($_.Exception.Response.StatusCode.value__ -eq 400)
}
Assert-True $dupNatBlocked "Duplicate national ID prevented with 400 Bad Request"

# 4.8 Assign Doctor 1 and 2 to Branch
$docBranch1 = Invoke-RestMethod -Uri "$baseUrl/api/admin/doctors/$doc1Id/branches/$branchId" -Method Post -Headers @{ Authorization = "Bearer $adminToken" }
$docBranch1Id = $docBranch1.id
Assert-True ($null -ne $docBranch1Id) "Doctor 1 assigned to Branch"

$docBranch2 = Invoke-RestMethod -Uri "$baseUrl/api/admin/doctors/$doc2Id/branches/$branchId" -Method Post -Headers @{ Authorization = "Bearer $adminToken" }
$docBranch2Id = $docBranch2.id
Assert-True ($null -ne $docBranch2Id) "Doctor 2 assigned to Branch"

# 4.9 Add Doctor 1 Schedule for today
$todayDayOfWeek = (Get-Date).DayOfWeek.ToString().ToUpper()
$schedBody = @{
    dayOfWeek = $todayDayOfWeek
    startTime = "08:00:00"
    endTime = "20:00:00"
    maxPatients = 50
} | ConvertTo-Json
$sched = Invoke-RestMethod -Uri "$baseUrl/api/admin/doctor-branches/$docBranch1Id/schedules" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $schedBody -ContentType "application/json"
Assert-True ($null -ne $sched.id) "Doctor 1 Schedule created for $todayDayOfWeek"

# Add Doctor 2 Schedule for today
$sched2 = Invoke-RestMethod -Uri "$baseUrl/api/admin/doctor-branches/$docBranch2Id/schedules" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $schedBody -ContentType "application/json"
Assert-True ($null -ne $sched2.id) "Doctor 2 Schedule created for $todayDayOfWeek"

# -----------------------------------------------------------------------------
# 5. Appointments and Concurrency/Overlapping
# -----------------------------------------------------------------------------
Write-Host "`n>>> 5. Appointments and Double-Booking..." -ForegroundColor Yellow

$apptDate = (Get-Date).ToString("yyyy-MM-dd")

# 5.1 Create Appointment for Doctor 1 (10:00 - 10:30)
$appt1Body = @{
    date = $apptDate
    startTime = "10:00:00"
    endTime = "10:30:00"
    notes = "Initial consult"
} | ConvertTo-Json
$appt1Uri = "$baseUrl/api/admin/appointments?doctorId=$doc1Id&patientId=$patientId&branchId=$branchId"
$appt1 = Invoke-RestMethod -Uri $appt1Uri -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $appt1Body -ContentType "application/json"
$appt1Id = $appt1.id
Assert-True ($null -ne $appt1Id) "Appointment 1 created with ID: $appt1Id"

# 5.2 Double-booking check: same doctor overlapping appointment (10:15 - 10:45) -> MUST FAIL
$doubleBookBlocked = $false
try {
    $overlapBody = @{
        date = $apptDate
        startTime = "10:15:00"
        endTime = "10:45:00"
        notes = "Conflicting consult"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri $appt1Uri -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $overlapBody -ContentType "application/json"
} catch {
    $doubleBookBlocked = $true
}
Assert-True $doubleBookBlocked "Double booking on same doctor prevented"

# 5.3 Different doctor at same time (10:00 - 10:30) -> MUST SUCCEED
$appt2Uri = "$baseUrl/api/admin/appointments?doctorId=$doc2Id&patientId=$patientId&branchId=$branchId"
$appt2 = Invoke-RestMethod -Uri $appt2Uri -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $appt1Body -ContentType "application/json"
Assert-True ($null -ne $appt2.id) "Different doctor booked at same time successfully"

# 5.4 Confirm Appointment 1
$confirmBody = @{ status = "CONFIRMED" } | ConvertTo-Json
$confirmedAppt = Invoke-RestMethod -Uri "$baseUrl/api/admin/appointments/$appt1Id/status" -Method Patch -Headers @{ Authorization = "Bearer $adminToken" } -Body $confirmBody -ContentType "application/json"
Assert-Equal $confirmedAppt.status "CONFIRMED" "Appointment 1 status changed to CONFIRMED"

# 5.5 Invalid status transition: CONFIRMED -> PENDING -> MUST FAIL
$invalidTransBlocked = $false
try {
    $invalidBody = @{ status = "PENDING" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/admin/appointments/$appt1Id/status" -Method Patch -Headers @{ Authorization = "Bearer $adminToken" } -Body $invalidBody -ContentType "application/json"
} catch {
    $invalidTransBlocked = ($_.Exception.Response.StatusCode.value__ -eq 400)
}
Assert-True $invalidTransBlocked "Invalid status transition (CONFIRMED -> PENDING) blocked with 400 Bad Request"

# -----------------------------------------------------------------------------
# 6. Medical Operations: Records, Prescriptions, Lab Results
# -----------------------------------------------------------------------------
Write-Host "`n>>> 6. Medical Operations..." -ForegroundColor Yellow

# 6.1 Medical Record
$medRecBody = @{
    appointmentId = $appt1Id
    diagnosis = "Acute Bronchitis"
    treatment = "Rest and antibiotic course"
    notes = "Follow up if fever persists"
} | ConvertTo-Json
$medRec = Invoke-RestMethod -Uri "$baseUrl/api/admin/medical-records" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $medRecBody -ContentType "application/json"
Assert-True ($null -ne $medRec.id) "Medical Record created successfully"

# 6.2 Medicine Creation
$medicineBody = @{
    name = "Amoxicillin 500mg $rand"
    description = "Broad-spectrum antibiotic"
    category = "ANTIBIOTICS"
    unit = "CAPSULE"
    price = 25.00
} | ConvertTo-Json
$medicine = Invoke-RestMethod -Uri "$baseUrl/api/admin/medicines" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $medicineBody -ContentType "application/json"
$medicineId = $medicine.id
Assert-True ($null -ne $medicineId) "Medicine created with ID: $medicineId"

# 6.3 Prescription Creation by Doctor 1
$rxBody = @{
    appointmentId = $appt1Id
    notes = "Take with food"
    items = @(
        @{
            medicineId = $medicineId
            dosage = "500mg"
            frequency = "3 times daily"
            duration = "7 days"
            instructions = "After meals"
        }
    )
} | ConvertTo-Json -Depth 5
$prescription = Invoke-RestMethod -Uri "$baseUrl/api/doctor/prescriptions" -Method Post -Headers @{ Authorization = "Bearer $doc1Token" } -Body $rxBody -ContentType "application/json"
$prescriptionId = $prescription.id
Assert-True ($null -ne $prescriptionId) "Prescription created by Doctor 1"

# 6.4 Duplicate Prescription prevention for same appointment
$dupRxBlocked = $false
try {
    Invoke-RestMethod -Uri "$baseUrl/api/doctor/prescriptions" -Method Post -Headers @{ Authorization = "Bearer $doc1Token" } -Body $rxBody -ContentType "application/json"
} catch {
    $dupRxBlocked = ($_.Exception.Response.StatusCode.value__ -eq 400)
}
Assert-True $dupRxBlocked "Duplicate prescription for same appointment prevented with 400 Bad Request"

# 6.5 Lab Test Creation
$labTestBody = @{
    name = "Complete Blood Count $rand"
    description = "CBC panel"
    price = 50.00
    referenceRange = "Standard"
} | ConvertTo-Json
$labTest = Invoke-RestMethod -Uri "$baseUrl/api/admin/lab-tests" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $labTestBody -ContentType "application/json"
$labTestId = $labTest.id
Assert-True ($null -ne $labTestId) "Lab Test created with ID: $labTestId"

# 6.6 Lab Order Creation by Doctor 1
$labOrderBody = @{
    appointmentId = $appt1Id
    labTestId = $labTestId
    notes = "Check WBC count"
} | ConvertTo-Json
$labOrder = Invoke-RestMethod -Uri "$baseUrl/api/doctor/lab-orders" -Method Post -Headers @{ Authorization = "Bearer $doc1Token" } -Body $labOrderBody -ContentType "application/json"
$labOrderId = $labOrder.id
Assert-True ($null -ne $labOrderId) "Lab Order created by Doctor 1"

# 6.7 Doctor Ownership: Doctor 2 trying to submit result for Doctor 1's order -> MUST FAIL
$wrongDocBlocked = $false
try {
    $resultBody = @{
        resultValue = "Normal"
        referenceRange = "4.5-11.0"
        notes = "All counts normal"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/doctor/lab-orders/$labOrderId/results" -Method Post -Headers @{ Authorization = "Bearer $doc2Token" } -Body $resultBody -ContentType "application/json"
} catch {
    $wrongDocBlocked = ($_.Exception.Response.StatusCode.value__ -eq 400)
}
Assert-True $wrongDocBlocked "Doctor 2 blocked from submitting result for Doctor 1's order (Ownership verified)"

# 6.8 Doctor 1 submits Lab Result -> MUST SUCCEED
$resultBody = @{
    resultValue = "10.2 x10^9/L"
    referenceRange = "4.5-11.0"
    notes = "WBC within normal limits"
} | ConvertTo-Json
$labResult = Invoke-RestMethod -Uri "$baseUrl/api/doctor/lab-orders/$labOrderId/results" -Method Post -Headers @{ Authorization = "Bearer $doc1Token" } -Body $resultBody -ContentType "application/json"
Assert-True ($null -ne $labResult.id) "Doctor 1 submitted Lab Result"

# -----------------------------------------------------------------------------
# 7. Billing, Payments and Idempotency
# -----------------------------------------------------------------------------
Write-Host "`n>>> 7. Billing, Payments and Idempotency..." -ForegroundColor Yellow

# 7.1 Receptionist creates Invoice
$invoiceBody = @{
    appointmentId = $appt1Id
    dueDate = (Get-Date).AddDays(7).ToString("yyyy-MM-dd")
    notes = "Consultation and tests"
    items = @(
        @{ description = "Consultation Fee"; amount = 100.00 },
        @{ description = "CBC Lab Test"; amount = 50.00 }
    )
} | ConvertTo-Json -Depth 5
$invoice = Invoke-RestMethod -Uri "$baseUrl/api/receptionist/invoices" -Method Post -Headers @{ Authorization = "Bearer $recToken" } -Body $invoiceBody -ContentType "application/json"
$invoiceId = $invoice.id
Assert-True ($null -ne $invoiceId) "Invoice created by Receptionist (ID: $invoiceId)"

# 7.2 Receptionist pays invoice with Idempotency-Key
$idempKey = "IDEMP-KEY-TEST-$rand"
$paymentBody = @{
    invoiceId = $invoiceId
    amount = 150.00
    method = "CARD"
    notes = "Full payment via credit card"
} | ConvertTo-Json

$paymentHeaders = @{
    Authorization = "Bearer $recToken"
    "Idempotency-Key" = $idempKey
}
$payment1 = Invoke-RestMethod -Uri "$baseUrl/api/receptionist/payments" -Method Post -Headers $paymentHeaders -Body $paymentBody -ContentType "application/json"
$payment1Id = $payment1.id
Assert-True ($null -ne $payment1Id) "Payment recorded successfully (ID: $payment1Id)"

# 7.3 Replay exact same request with SAME Idempotency-Key -> MUST return identical response without duplicate payment
$payment2 = Invoke-RestMethod -Uri "$baseUrl/api/receptionist/payments" -Method Post -Headers $paymentHeaders -Body $paymentBody -ContentType "application/json"
Assert-Equal $payment2.id $payment1Id "Idempotent replay returned exact same payment ID: $payment1Id"
Assert-Equal $payment2.paymentReference $payment1.paymentReference "Payment reference matches on replay"

# Verify invoice payments count is exactly 1 (no duplicate row)
$invPayments = Invoke-RestMethod -Uri "$baseUrl/api/receptionist/payments/invoice/$invoiceId" -Method Get -Headers @{ Authorization = "Bearer $recToken" }
Assert-Equal $invPayments.Count 1 "Invoice has exactly 1 payment record (no duplicate row created)"

# -----------------------------------------------------------------------------
# 8. Notifications and Notification Security
# -----------------------------------------------------------------------------
Write-Host "`n>>> 8. Notifications and Notification Security..." -ForegroundColor Yellow

# Patient logs in with the created patient account to check notifications
$aliceLoginBody = @{ email = "alice.smith$rand@hospital.com"; password = "PatientPassword123!" } | ConvertTo-Json
$aliceAuth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $aliceLoginBody -ContentType "application/json"
$aliceToken = $aliceAuth.accessToken

# Give async event listener 1 second to complete persistence
Start-Sleep -Seconds 1

$notifs = Invoke-RestMethod -Uri "$baseUrl/api/notifications" -Method Get -Headers @{ Authorization = "Bearer $aliceToken" }
Assert-True ($notifs.content.Count -gt 0) "Patient received notifications ($($notifs.content.Count) notifications found)"

$firstNotif = $notifs.content[0]
$firstNotifId = $firstNotif.id

# Mark notification as read
$readRes = Invoke-WebRequest -Uri "$baseUrl/api/notifications/$firstNotifId/read" -Method Patch -Headers @{ Authorization = "Bearer $aliceToken" }
Assert-Equal $readRes.StatusCode 204 "Patient marked notification $firstNotifId as read (204 No Content)"

# IDOR Security: Doctor 1 tries to mark Alice's notification as read -> MUST return 404 (ResourceNotFoundException / ownership check)
$idorBlocked = $false
try {
    Invoke-RestMethod -Uri "$baseUrl/api/notifications/$firstNotifId/read" -Method Patch -Headers @{ Authorization = "Bearer $doc1Token" }
} catch {
    $idorBlocked = ($_.Exception.Response.StatusCode.value__ -eq 404)
}
Assert-True $idorBlocked "Doctor trying to mark Alice's notification as read rejected with 404 Not Found (IDOR protected)"

Write-Host "`n=== COMPREHENSIVE TEST SUITE COMPLETED SUCCESSFULLY ===" -ForegroundColor Green
