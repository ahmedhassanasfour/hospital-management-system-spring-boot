#!/usr/bin/env python3
"""
comprehensive_test_suite.py
Final Comprehensive Testing Suite for Hospital Management System (Task 13)
"""

import sys
import time
import uuid
import random
import requests
import subprocess
from datetime import datetime, date, timedelta

BASE_URL = "http://localhost:8081"

results = []

def record(suite: str, test_name: str, passed: bool, details: str):
    status = "PASS" if passed else "FAIL"
    results.append({
        "suite": suite,
        "test": test_name,
        "status": status,
        "details": details
    })
    prefix = "[PASS]" if passed else "[FAIL]"
    print(f"  {prefix} {test_name}: {details}")

print("=" * 70)
print("   HOSPITAL MANAGEMENT SYSTEM - TASK 13 COMPREHENSIVE VALIDATION")
print("=" * 70)

# -----------------------------------------------------------------------------
# 1. REAL ENVIRONMENT & ACTUATOR READINESS
# -----------------------------------------------------------------------------
print("\n>>> 1. Real Environment & Actuator Probes...")

# Wait up to 30s for readiness
ready = False
for _ in range(15):
    try:
        r = requests.get(f"{BASE_URL}/actuator/health/readiness", timeout=3)
        if r.status_code == 200 and r.json().get("status") == "UP":
            ready = True
            break
    except Exception:
        time.sleep(2)

record("Environment", "Application Readiness Probe", ready, "App is UP and ready")

try:
    r = requests.get(f"{BASE_URL}/actuator/health", timeout=3)
    record("Actuator", "Root /actuator/health", r.status_code == 200 and r.json().get("status") == "UP", f"HTTP {r.status_code}, status={r.json().get('status')}")
except Exception as e:
    record("Actuator", "Root /actuator/health", False, str(e))

try:
    r = requests.get(f"{BASE_URL}/actuator/health/liveness", timeout=3)
    record("Actuator", "Liveness Probe", r.status_code == 200 and r.json().get("status") == "UP", f"HTTP {r.status_code}, status={r.json().get('status')}")
except Exception as e:
    record("Actuator", "Liveness Probe", False, str(e))

try:
    r = requests.get(f"{BASE_URL}/actuator/info", timeout=3)
    record("Actuator", "/actuator/info Endpoint", r.status_code == 200, f"HTTP {r.status_code}, response={r.text}")
except Exception as e:
    record("Actuator", "/actuator/info Endpoint", False, str(e))

# -----------------------------------------------------------------------------
# 2. AUTHENTICATION & AUTHORIZATION
# -----------------------------------------------------------------------------
print("\n>>> 2. Authentication & Authorization...")

rand_id = random.randint(10000, 99999)
patient_email = f"patient.{rand_id}@hospital.com"
patient_pass = "PatientPass123!"

# Register Patient
reg_payload = {
    "firstName": "John",
    "lastName": f"Doe{rand_id}",
    "email": patient_email,
    "password": patient_pass
}
r_reg = requests.post(f"{BASE_URL}/api/auth/register", json=reg_payload)
reg_json = r_reg.json() if r_reg.status_code in (200, 201) else {}
no_pw_in_reg = "password" not in reg_json or reg_json.get("password") is None
record("Auth", "Register Patient", r_reg.status_code in (200, 201) and no_pw_in_reg, f"HTTP {r_reg.status_code}, password omitted: {no_pw_in_reg}")

# Login Patient
r_pat_login = requests.post(f"{BASE_URL}/api/auth/login", json={"email": patient_email, "password": patient_pass})
pat_tokens = r_pat_login.json() if r_pat_login.status_code == 200 else {}
patient_token = pat_tokens.get("accessToken")
patient_refresh = pat_tokens.get("refreshToken")
record("Auth", "Login Patient", r_pat_login.status_code == 200 and bool(patient_token), f"HTTP {r_pat_login.status_code}, accessToken acquired")

# Login Admin
r_admin_login = requests.post(f"{BASE_URL}/api/auth/login", json={"email": "admin@hospital.com", "password": "ChangeMe@Local123"})
admin_token = r_admin_login.json().get("accessToken") if r_admin_login.status_code == 200 else None
record("Auth", "Login Admin", r_admin_login.status_code == 200 and bool(admin_token), f"HTTP {r_admin_login.status_code}, adminToken acquired")

# Login Receptionist
r_rec_login = requests.post(f"{BASE_URL}/api/auth/login", json={"email": "receptionist@hospital.com", "password": "ChangeMe@Local123"})
rec_token = r_rec_login.json().get("accessToken") if r_rec_login.status_code == 200 else None
record("Auth", "Login Receptionist", r_rec_login.status_code == 200 and bool(rec_token), f"HTTP {r_rec_login.status_code}, recToken acquired")

# Refresh Token
r_ref = requests.post(f"{BASE_URL}/api/auth/refresh", json={"refreshToken": patient_refresh})
new_access = r_ref.json().get("accessToken") if r_ref.status_code == 200 else None
record("Auth", "Refresh Token Flow", r_ref.status_code == 200 and bool(new_access), f"HTTP {r_ref.status_code}, new token generated")

# Invalid JWT -> 401 or 403
r_inv_jwt = requests.get(f"{BASE_URL}/api/users/me", headers={"Authorization": "Bearer invalid.jwt.token"})
record("Auth", "Invalid JWT Rejection", r_inv_jwt.status_code in (401, 403), f"HTTP {r_inv_jwt.status_code}")

# Invalid Refresh Token -> 400 or 401
r_inv_ref = requests.post(f"{BASE_URL}/api/auth/refresh", json={"refreshToken": "completely-invalid-refresh-token"})
record("Auth", "Invalid Refresh Token Rejection", r_inv_ref.status_code in (400, 401), f"HTTP {r_inv_ref.status_code}")

# RBAC: Patient forbidden on Admin endpoint -> 403
r_rbac_pat = requests.get(f"{BASE_URL}/api/admin/doctors", headers={"Authorization": f"Bearer {patient_token}"})
record("Auth", "RBAC: Patient on Admin Forbidden", r_rbac_pat.status_code == 403, f"HTTP {r_rbac_pat.status_code} Forbidden")

# RBAC: Receptionist forbidden on Admin endpoint -> 403
r_rbac_rec = requests.get(f"{BASE_URL}/api/admin/doctors", headers={"Authorization": f"Bearer {rec_token}"})
record("Auth", "RBAC: Receptionist on Admin Forbidden", r_rbac_rec.status_code == 403, f"HTTP {r_rbac_rec.status_code} Forbidden")

admin_headers = {"Authorization": f"Bearer {admin_token}"}
rec_headers = {"Authorization": f"Bearer {rec_token}"}

# -----------------------------------------------------------------------------
# 3. CORE ENTITIES
# -----------------------------------------------------------------------------
print("\n>>> 3. Core Entities Management...")

# Create Branch
branch_payload = {
    "name": f"Metropolitan General {rand_id}",
    "address": "500 Healthcare Way",
    "phone": f"+1000{rand_id}",
    "email": f"metro{rand_id}@hospital.com"
}
r_br = requests.post(f"{BASE_URL}/api/admin/branches", json=branch_payload, headers=admin_headers)
branch_id = r_br.json().get("id") if r_br.status_code in (200, 201) else None
record("Core", "Create Branch", bool(branch_id), f"HTTP {r_br.status_code}, branchId={branch_id}")

# Duplicate Branch -> 400
r_dup_br = requests.post(f"{BASE_URL}/api/admin/branches", json=branch_payload, headers=admin_headers)
record("Core", "Duplicate Branch Prevention", r_dup_br.status_code in (400, 409), f"HTTP {r_dup_br.status_code}")

# Create Doctor 1 (Diagnostics)
doc1_payload = {
    "firstName": "Gregory",
    "lastName": f"House {rand_id}",
    "email": f"dr.house.{rand_id}@hospital.com",
    "password": "DocPassword123!",
    "specialization": "Diagnostics",
    "licenseNumber": f"LIC-H-{rand_id}",
    "phone": f"+111{rand_id}",
    "bio": "Diagnostics Chief"
}
r_d1 = requests.post(f"{BASE_URL}/api/admin/doctors", json=doc1_payload, headers=admin_headers)
doc1_id = r_d1.json().get("id") if r_d1.status_code in (200, 201) else None

# Doctor 1 Login
r_d1_login = requests.post(f"{BASE_URL}/api/auth/login", json={"email": doc1_payload["email"], "password": "DocPassword123!"})
doc1_token = r_d1_login.json().get("accessToken")
doc1_headers = {"Authorization": f"Bearer {doc1_token}"}
record("Core", "Create & Authenticate Doctor 1", bool(doc1_id and doc1_token), f"Doctor 1 ID: {doc1_id}")

# Duplicate Doctor License -> 400
dup_lic_payload = doc1_payload.copy()
dup_lic_payload["email"] = f"clone.{rand_id}@hospital.com"
r_dup_lic = requests.post(f"{BASE_URL}/api/admin/doctors", json=dup_lic_payload, headers=admin_headers)
record("Core", "Duplicate Doctor License Prevention", r_dup_lic.status_code in (400, 409), f"HTTP {r_dup_lic.status_code}")

# Create Doctor 2 (Oncology)
doc2_payload = {
    "firstName": "James",
    "lastName": f"Wilson {rand_id}",
    "email": f"dr.wilson.{rand_id}@hospital.com",
    "password": "DocPassword123!",
    "specialization": "Oncology",
    "licenseNumber": f"LIC-W-{rand_id}",
    "phone": f"+222{rand_id}",
    "bio": "Oncology Head"
}
r_d2 = requests.post(f"{BASE_URL}/api/admin/doctors", json=doc2_payload, headers=admin_headers)
doc2_id = r_d2.json().get("id") if r_d2.status_code in (200, 201) else None

# Doctor 2 Login
r_d2_login = requests.post(f"{BASE_URL}/api/auth/login", json={"email": doc2_payload["email"], "password": "DocPassword123!"})
doc2_token = r_d2_login.json().get("accessToken")
doc2_headers = {"Authorization": f"Bearer {doc2_token}"}
record("Core", "Create & Authenticate Doctor 2", bool(doc2_id and doc2_token), f"Doctor 2 ID: {doc2_id}")

# Create Patient (Alice)
patient_payload = {
    "firstName": "Alice",
    "lastName": f"Smith {rand_id}",
    "email": f"alice.{rand_id}@hospital.com",
    "password": "PatientPass123!",
    "nationalId": f"NAT-{rand_id}-ALICE",
    "phone": f"+555{rand_id}",
    "dateOfBirth": "1994-06-20",
    "gender": "FEMALE",
    "address": "123 Cherry Lane"
}
r_pat = requests.post(f"{BASE_URL}/api/admin/patients", json=patient_payload, headers=admin_headers)
patient_entity_id = r_pat.json().get("id") if r_pat.status_code in (200, 201) else None

# Alice Login
r_alice_login = requests.post(f"{BASE_URL}/api/auth/login", json={"email": patient_payload["email"], "password": "PatientPass123!"})
alice_token = r_alice_login.json().get("accessToken")
alice_headers = {"Authorization": f"Bearer {alice_token}"}
record("Core", "Create Patient (Alice)", bool(patient_entity_id and alice_token), f"Patient ID: {patient_entity_id}")

# Duplicate National ID -> 400
dup_nat_payload = patient_payload.copy()
dup_nat_payload["email"] = f"dup.nat.{rand_id}@hospital.com"
r_dup_nat = requests.post(f"{BASE_URL}/api/admin/patients", json=dup_nat_payload, headers=admin_headers)
record("Core", "Duplicate National ID Prevention", r_dup_nat.status_code in (400, 409), f"HTTP {r_dup_nat.status_code}")

# Assign Doctors to Branch
r_as1 = requests.post(f"{BASE_URL}/api/admin/doctors/{doc1_id}/branches/{branch_id}", headers=admin_headers)
r_as2 = requests.post(f"{BASE_URL}/api/admin/doctors/{doc2_id}/branches/{branch_id}", headers=admin_headers)
record("Core", "Doctor Branch Assignments", r_as1.status_code in (200, 201) and r_as2.status_code in (200, 201), f"Docs assigned to Branch {branch_id}")

# Create Schedules for Today
today_dow = datetime.now().strftime("%A").upper()
sched_payload = {
    "dayOfWeek": today_dow,
    "startTime": "08:00:00",
    "endTime": "20:00:00"
}
r_sc1 = requests.post(f"{BASE_URL}/api/admin/doctors/{doc1_id}/branches/{branch_id}/schedules", json=sched_payload, headers=admin_headers)
r_sc2 = requests.post(f"{BASE_URL}/api/admin/doctors/{doc2_id}/branches/{branch_id}/schedules", json=sched_payload, headers=admin_headers)
record("Core", "Doctor Schedules Created", r_sc1.status_code in (200, 201) and r_sc2.status_code in (200, 201), f"Schedules active for {today_dow}")

# -----------------------------------------------------------------------------
# 4. APPOINTMENTS & CONCURRENCY / LOCKING
# -----------------------------------------------------------------------------
print("\n>>> 4. Appointments & Concurrency...")

today_str = date.today().isoformat()

# Create Appointment 1 for Doctor 1 (10:00 - 10:30)
appt1_payload = {
    "date": today_str,
    "startTime": "10:00:00",
    "endTime": "10:30:00",
    "notes": "Consultation with Dr. House"
}
r_ap1 = requests.post(f"{BASE_URL}/api/admin/appointments/doctors/{doc1_id}/patients/{patient_entity_id}/branches/{branch_id}", json=appt1_payload, headers=admin_headers)
appt1_id = r_ap1.json().get("id") if r_ap1.status_code in (200, 201) else None
record("Appointments", "Create Appointment 1", bool(appt1_id), f"HTTP {r_ap1.status_code}, Appt ID: {appt1_id}")

# Double-booking check: Same doctor overlapping slot (10:15 - 10:45) -> MUST FAIL (400 or 409)
overlap_payload = {
    "date": today_str,
    "startTime": "10:15:00",
    "endTime": "10:45:00",
    "notes": "Double booking attempt"
}
r_overlap = requests.post(f"{BASE_URL}/api/admin/appointments/doctors/{doc1_id}/patients/{patient_entity_id}/branches/{branch_id}", json=overlap_payload, headers=admin_headers)
record("Appointments", "Double-Booking Prevention (Same Doctor)", r_overlap.status_code in (400, 409), f"Blocked with HTTP {r_overlap.status_code}")

# Different doctor at same time slot (10:00 - 10:30) -> MUST SUCCEED
r_ap2 = requests.post(f"{BASE_URL}/api/admin/appointments/doctors/{doc2_id}/patients/{patient_entity_id}/branches/{branch_id}", json=appt1_payload, headers=admin_headers)
appt2_id = r_ap2.json().get("id") if r_ap2.status_code in (200, 201) else None
record("Appointments", "Concurrent Booking (Different Doctor)", bool(appt2_id), f"HTTP {r_ap2.status_code}, Appt 2 ID: {appt2_id}")

# Status Transition: PENDING -> CONFIRMED
r_conf = requests.patch(f"{BASE_URL}/api/admin/appointments/{appt1_id}/status", json={"status": "CONFIRMED"}, headers=admin_headers)
record("Appointments", "Status Transition (CONFIRMED)", r_conf.status_code == 200 and r_conf.json().get("status") == "CONFIRMED", f"HTTP {r_conf.status_code}")

# Invalid Status Transition: CONFIRMED -> PENDING -> MUST FAIL (400)
r_inv_stat = requests.patch(f"{BASE_URL}/api/admin/appointments/{appt1_id}/status", json={"status": "PENDING"}, headers=admin_headers)
record("Appointments", "Invalid Status Transition Rejection", r_inv_stat.status_code == 400, f"HTTP {r_inv_stat.status_code}")

# Status Transition: PENDING -> CANCELLED on Appointment 2
r_canc = requests.patch(f"{BASE_URL}/api/admin/appointments/{appt2_id}/status", json={"status": "CANCELLED"}, headers=admin_headers)
record("Appointments", "Status Transition (CANCELLED)", r_canc.status_code == 200 and r_canc.json().get("status") == "CANCELLED", f"HTTP {r_canc.status_code}")

# -----------------------------------------------------------------------------
# 5. MEDICAL OPERATIONS
# -----------------------------------------------------------------------------
print("\n>>> 5. Medical Operations...")

# Medical Record
rec_payload = {
    "diagnosis": "Acute Bronchitis",
    "symptoms": "Severe cough and mild fever",
    "notes": "Patient advised 5 days rest",
    "treatment": "Antibiotics and hydration"
}
r_mr = requests.post(f"{BASE_URL}/api/admin/medical-records/appointments/{appt1_id}", json=rec_payload, headers=admin_headers)
record_id = r_mr.json().get("id") if r_mr.status_code in (200, 201) else None
record("Medical", "Create Medical Record", bool(record_id), f"HTTP {r_mr.status_code}, Record ID: {record_id}")

# Create Medicine
med_payload = {
    "name": f"Amoxicillin 500mg {rand_id}",
    "description": "Standard broad spectrum antibiotic"
}
r_med = requests.post(f"{BASE_URL}/api/medicines", json=med_payload, headers=admin_headers)
med_id = r_med.json().get("id") if r_med.status_code in (200, 201) else None
record("Medical", "Create Medicine in Catalogue", bool(med_id), f"HTTP {r_med.status_code}, Medicine ID: {med_id}")

# Create Prescription by Doctor 1
rx_payload = {
    "appointmentId": appt1_id,
    "notes": "Take capsules after meals",
    "items": [
        {
            "medicineId": med_id,
            "dosage": "500mg",
            "frequency": "Three times daily",
            "duration": "7 days",
            "instructions": "Drink plenty of fluids"
        }
    ]
}
r_rx = requests.post(f"{BASE_URL}/api/doctor/prescriptions", json=rx_payload, headers=doc1_headers)
rx_id = r_rx.json().get("id") if r_rx.status_code in (200, 201) else None
record("Medical", "Create Prescription (Doctor 1)", bool(rx_id), f"HTTP {r_rx.status_code}, Prescription ID: {rx_id}")

# Duplicate Prescription for same appointment -> 400
r_dup_rx = requests.post(f"{BASE_URL}/api/doctor/prescriptions", json=rx_payload, headers=doc1_headers)
record("Medical", "Duplicate Prescription Prevention", r_dup_rx.status_code == 400, f"HTTP {r_dup_rx.status_code}")

# Create Lab Test
lt_payload = {
    "name": f"Complete Blood Count {rand_id}",
    "description": "Full hematology evaluation"
}
r_lt = requests.post(f"{BASE_URL}/api/admin/lab-tests", json=lt_payload, headers=admin_headers)
lab_test_id = r_lt.json().get("id") if r_lt.status_code in (200, 201) else None
record("Medical", "Create Lab Test in Catalogue", bool(lab_test_id), f"HTTP {r_lt.status_code}, Lab Test ID: {lab_test_id}")

# Doctor 1 creates Lab Order
lo_payload = {
    "appointmentId": appt1_id,
    "labTestId": lab_test_id,
    "notes": "Check leukocyte count"
}
r_lo = requests.post(f"{BASE_URL}/api/doctor/lab-orders", json=lo_payload, headers=doc1_headers)
lab_order_id = r_lo.json().get("id") if r_lo.status_code in (200, 201) else None
record("Medical", "Create Lab Order (Doctor 1)", bool(lab_order_id), f"HTTP {r_lo.status_code}, Lab Order ID: {lab_order_id}")

# Doctor 2 attempts to record result for Doctor 1's order -> MUST FAIL (400 or 403)
res_payload = {
    "resultValue": "WBC 7.5 x10^9/L",
    "referenceRange": "4.5 - 11.0",
    "notes": "Normal findings"
}
r_wrong_doc = requests.post(f"{BASE_URL}/api/doctor/lab-results/lab-orders/{lab_order_id}", json=res_payload, headers=doc2_headers)
record("Medical", "Doctor Ownership on Lab Result", r_wrong_doc.status_code in (400, 403), f"Blocked with HTTP {r_wrong_doc.status_code}")

# Doctor 1 records Lab Result -> MUST SUCCEED (201)
r_res = requests.post(f"{BASE_URL}/api/doctor/lab-results/lab-orders/{lab_order_id}", json=res_payload, headers=doc1_headers)
lab_res_id = r_res.json().get("id") if r_res.status_code in (200, 201) else None
record("Medical", "Record Lab Result (Doctor 1)", bool(lab_res_id), f"HTTP {r_res.status_code}, Lab Result ID: {lab_res_id}")

# File Upload: Valid PNG
png_bytes = b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15c4"
files = {"file": ("test_scan.png", png_bytes, "image/png")}
r_file = requests.post(f"{BASE_URL}/api/patient/files/documents", files=files, headers=alice_headers)
file_id = r_file.json().get("id") if r_file.status_code in (200, 201) else None
record("Files", "Valid Medical Document Upload", bool(file_id), f"HTTP {r_file.status_code}, File ID: {file_id}")

# File Upload: Invalid type (text/plain) -> MUST FAIL (400)
bad_files = {"file": ("malicious.txt", b"plain text data", "text/plain")}
r_bad_file = requests.post(f"{BASE_URL}/api/patient/files/documents", files=bad_files, headers=alice_headers)
record("Files", "Invalid File Type Rejection", r_bad_file.status_code == 400, f"HTTP {r_bad_file.status_code}")

# File Download: IDOR security (Patient 1 cannot download Alice's file) -> MUST FAIL (400, 403, 404)
r_idor_dl = requests.get(f"{BASE_URL}/api/patient/files/{file_id}/download", headers={"Authorization": f"Bearer {patient_token}"})
record("Files", "File Download IDOR Protection", r_idor_dl.status_code in (400, 403, 404), f"HTTP {r_idor_dl.status_code}")

# -----------------------------------------------------------------------------
# 6. BILLING & PAYMENTS + IDEMPOTENCY
# -----------------------------------------------------------------------------
print("\n>>> 6. Billing, Payments & Idempotency...")

# Receptionist creates Invoice
inv_payload = {
    "patientId": patient_entity_id,
    "appointmentId": appt1_id,
    "notes": "Full diagnostic consultation and blood panel",
    "dueAt": (datetime.now() + timedelta(days=14)).isoformat(),
    "items": [
        {
            "type": "CONSULTATION",
            "description": "Specialist Consultation",
            "quantity": 1.0,
            "unitPrice": 120.00
        },
        {
            "type": "LAB",
            "description": "CBC Hematology Panel",
            "quantity": 1.0,
            "unitPrice": 80.00,
            "labOrderId": lab_order_id
        }
    ]
}
r_inv = requests.post(f"{BASE_URL}/api/receptionist/invoices", json=inv_payload, headers=rec_headers)
inv_id = r_inv.json().get("id") if r_inv.status_code in (200, 201) else None
total_amt = r_inv.json().get("totalAmount") if r_inv.status_code in (200, 201) else None
record("Billing", "Create Invoice", bool(inv_id), f"HTTP {r_inv.status_code}, Invoice ID: {inv_id}, Total: {total_amt}")

# Invalid Payment Amount (-50.00) -> MUST FAIL (400)
bad_pay_payload = {
    "invoiceId": inv_id,
    "amount": -50.00,
    "method": "CARD"
}
r_bad_pay = requests.post(f"{BASE_URL}/api/receptionist/payments", json=bad_pay_payload, headers={**rec_headers, "Idempotency-Key": f"BAD-{rand_id}"})
record("Billing", "Invalid Payment Amount Rejection", r_bad_pay.status_code == 400, f"HTTP {r_bad_pay.status_code}")

# Idempotent Payment 1
idemp_key = f"PAY-KEY-{rand_id}-COMPREHENSIVE"
pay_payload = {
    "invoiceId": inv_id,
    "amount": 200.00,
    "method": "CARD",
    "notes": "Full payment settled via card"
}
pay_headers = {**rec_headers, "Idempotency-Key": idemp_key}
r_pay1 = requests.post(f"{BASE_URL}/api/receptionist/payments", json=pay_payload, headers=pay_headers)
pay1_json = r_pay1.json() if r_pay1.status_code in (200, 201) else {}
pay1_id = pay1_json.get("id")
pay1_ref = pay1_json.get("paymentReference")
record("Billing", "Payment 1 with Idempotency-Key", bool(pay1_id), f"HTTP {r_pay1.status_code}, Pay ID: {pay1_id}, Ref: {pay1_ref}")

# Idempotent Replay with exact same Idempotency-Key
r_pay2 = requests.post(f"{BASE_URL}/api/receptionist/payments", json=pay_payload, headers=pay_headers)
pay2_json = r_pay2.json() if r_pay2.status_code in (200, 201) else {}
exact_replay = (r_pay2.status_code in (200, 201) and pay2_json.get("id") == pay1_id and pay2_json.get("paymentReference") == pay1_ref)
record("Billing", "Idempotent Replay Match", exact_replay, f"Returned identical payment ID {pay2_json.get('id')}")

# Verify exactly 1 payment record exists for this invoice
r_inv_pays = requests.get(f"{BASE_URL}/api/receptionist/payments/invoice/{inv_id}", headers=rec_headers)
pays_list = r_inv_pays.json() if r_inv_pays.status_code == 200 else []
record("Billing", "No Duplicate Payment Row in DB", len(pays_list) == 1, f"Invoice has exactly {len(pays_list)} payment record(s)")

# Attempt to pay already-paid invoice with a NEW key -> MUST FAIL (400)
r_overpay = requests.post(f"{BASE_URL}/api/receptionist/payments", json={"invoiceId": inv_id, "amount": 50.00, "method": "CASH"}, headers={**rec_headers, "Idempotency-Key": f"NEW-KEY-{rand_id}"})
record("Billing", "Already-Paid Invoice Rejection", r_overpay.status_code == 400, f"HTTP {r_overpay.status_code}")

# -----------------------------------------------------------------------------
# 7. REDIS CACHE
# -----------------------------------------------------------------------------
print("\n>>> 7. Redis Cache Verification...")

# 1st request -> Miss & Cache
r_cache1 = requests.get(f"{BASE_URL}/api/medicines", headers=admin_headers)
# 2nd request -> Cache Hit
r_cache2 = requests.get(f"{BASE_URL}/api/medicines", headers=admin_headers)
cache_accessible = r_cache1.status_code == 200 and r_cache2.status_code == 200 and len(r_cache2.json()) > 0
record("Redis", "Cached Endpoint Access & Serialization", cache_accessible, f"HTTP {r_cache2.status_code}, items count={len(r_cache2.json()) if cache_accessible else 0}")

# Verify Redis contains cached keys via docker
redis_keys_proc = subprocess.run(["docker", "exec", "hospital-redis", "redis-cli", "keys", "*medicines*"], capture_output=True, text=True)
redis_keys = [k.strip() for k in redis_keys_proc.stdout.strip().split("\n") if k.strip()]
has_redis_key = any("medicines" in k for k in redis_keys)
record("Redis", "Redis Container Key Presence", has_redis_key, f"Keys found: {', '.join(redis_keys)}")

# -----------------------------------------------------------------------------
# 8. NOTIFICATIONS & SECURITY / IDOR
# -----------------------------------------------------------------------------
print("\n>>> 8. Notifications & Security...")

# Wait 2s for async side effects to persist
time.sleep(2)

r_notifs = requests.get(f"{BASE_URL}/api/notifications", headers=alice_headers)
notif_items = r_notifs.json().get("content", []) if r_notifs.status_code == 200 else []
record("Notifications", "Patient Receives Event Notifications", len(notif_items) >= 4, f"Alice received {len(notif_items)} notification(s)")

if notif_items:
    alice_notif_id = notif_items[0]["id"]
    
    # Alice marks notification as read -> 204
    r_read = requests.patch(f"{BASE_URL}/api/notifications/{alice_notif_id}/read", headers=alice_headers)
    record("Notifications", "Mark Own Notification as Read", r_read.status_code == 204, f"HTTP {r_read.status_code}")

    # IDOR: Another patient attempts to mark Alice's notification -> 404 (ResourceNotFoundException)
    r_idor_notif = requests.patch(f"{BASE_URL}/api/notifications/{alice_notif_id}/read", headers={"Authorization": f"Bearer {patient_token}"})
    record("Notifications", "IDOR: Other User Notification Modification", r_idor_notif.status_code in (403, 404), f"HTTP {r_idor_notif.status_code}")

# -----------------------------------------------------------------------------
# 9. AUDIT LOGGING
# -----------------------------------------------------------------------------
print("\n>>> 9. Audit Logging Verification...")

audit_proc = subprocess.run([
    "docker", "exec", "hospital-postgres",
    "psql", "-U", "postgres", "-d", "hospital_db",
    "-t", "-c", "SELECT DISTINCT action FROM audit_logs ORDER BY action;"
], capture_output=True, text=True)

recorded_actions = [a.strip() for a in audit_proc.stdout.strip().split("\n") if a.strip()]
required_actions = [
    "USER_REGISTERED", "USER_LOGIN", "DOCTOR_CREATED", "PATIENT_CREATED",
    "DOCTOR_BRANCH_ASSIGNED", "APPOINTMENT_CREATED", "APPOINTMENT_STATUS_CHANGED",
    "PAYMENT_CREATED", "PRESCRIPTION_CREATED", "LAB_RESULT_CREATED"
]
missing_actions = [a for a in required_actions if a not in recorded_actions]
audit_passed = len(missing_actions) == 0
record("Audit", "Business Audit Actions Recorded", audit_passed, f"Recorded {len(recorded_actions)} action types. Missing: {missing_actions}")

# -----------------------------------------------------------------------------
# SUMMARY & FINAL TALLY
# -----------------------------------------------------------------------------
print("\n" + "=" * 70)
print("                       VALIDATION SUMMARY")
print("=" * 70)

passed_count = sum(1 for t in results if t["status"] == "PASS")
failed_count = sum(1 for t in results if t["status"] == "FAIL")
total_count = len(results)

print(f"Total Checks: {total_count} | PASSED: {passed_count} | FAILED: {failed_count}\n")

if failed_count == 0:
    print(">>> ALL COMPREHENSIVE TESTS PASSED SUCCESSFULLY! <<<\n")
    sys.exit(0)
else:
    print(">>> SOME TESTS FAILED. Review details above. <<<\n")
    sys.exit(1)
