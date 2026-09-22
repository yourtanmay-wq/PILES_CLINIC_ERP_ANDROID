package com.tkbiswas.pilesclinic.data.repository

import com.tkbiswas.pilesclinic.data.local.EnquiryEntity
import com.tkbiswas.pilesclinic.data.local.FollowUpEntity
import com.tkbiswas.pilesclinic.data.local.PaymentEntity
import com.tkbiswas.pilesclinic.data.local.RegistrationEntity
import com.tkbiswas.pilesclinic.data.remote.EnquiryDto
import com.tkbiswas.pilesclinic.data.remote.FollowUpDto
import com.tkbiswas.pilesclinic.data.remote.PaymentDto
import com.tkbiswas.pilesclinic.data.remote.RegistrationDto

fun EnquiryEntity.toDto() = EnquiryDto(
    id = id, patientName = patientName, phone = phone, address = address,
    enquiryDate = enquiryDate, source = source, notes = notes,
    createdAt = createdAt, updatedAt = updatedAt, isDeleted = isDeleted
)

fun EnquiryDto.toEntity(syncStatus: com.tkbiswas.pilesclinic.data.local.SyncStatus) = EnquiryEntity(
    id = id, patientName = patientName, phone = phone, address = address,
    enquiryDate = enquiryDate, source = source, notes = notes,
    createdAt = createdAt, updatedAt = updatedAt, isDeleted = isDeleted,
    syncStatus = syncStatus
)

fun RegistrationEntity.toDto() = RegistrationDto(
    id = id, enquiryId = enquiryId, regNo = regNo, patientName = patientName,
    age = age, gender = gender, phone = phone, address = address,
    registrationDate = registrationDate, referredBy = referredBy, notes = notes,
    createdAt = createdAt, updatedAt = updatedAt, isDeleted = isDeleted
)

fun RegistrationDto.toEntity(syncStatus: com.tkbiswas.pilesclinic.data.local.SyncStatus) = RegistrationEntity(
    id = id, enquiryId = enquiryId, regNo = regNo, patientName = patientName,
    age = age, gender = gender, phone = phone, address = address,
    registrationDate = registrationDate, referredBy = referredBy, notes = notes,
    createdAt = createdAt, updatedAt = updatedAt, isDeleted = isDeleted,
    syncStatus = syncStatus
)

fun FollowUpEntity.toDto() = FollowUpDto(
    id = id, patientId = patientId, followUpDate = followUpDate, notes = notes,
    nextVisitDate = nextVisitDate, doneByRole = doneByRole,
    createdAt = createdAt, updatedAt = updatedAt, isDeleted = isDeleted
)

fun FollowUpDto.toEntity(syncStatus: com.tkbiswas.pilesclinic.data.local.SyncStatus) = FollowUpEntity(
    id = id, patientId = patientId, followUpDate = followUpDate, notes = notes,
    nextVisitDate = nextVisitDate, doneByRole = doneByRole,
    createdAt = createdAt, updatedAt = updatedAt, isDeleted = isDeleted,
    syncStatus = syncStatus
)

fun PaymentEntity.toDto() = PaymentDto(
    id = id, patientId = patientId, amount = amount, paymentDate = paymentDate,
    mode = mode, purpose = purpose, receivedByRole = receivedByRole,
    createdAt = createdAt, updatedAt = updatedAt, isDeleted = isDeleted
)

fun PaymentDto.toEntity(syncStatus: com.tkbiswas.pilesclinic.data.local.SyncStatus) = PaymentEntity(
    id = id, patientId = patientId, amount = amount, paymentDate = paymentDate,
    mode = mode, purpose = purpose, receivedByRole = receivedByRole,
    createdAt = createdAt, updatedAt = updatedAt, isDeleted = isDeleted,
    syncStatus = syncStatus
)
