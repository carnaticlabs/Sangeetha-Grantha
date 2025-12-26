# Local Network Testing Guide - Mobile File Upload

TODO: Update this document for Sangita Grantha.

## Overview

TODO: Update this section.

## Prerequisites

TODO: Update this section.

### Development Machine Setup

TODO: Update this section.

### Mobile Devices

TODO: Update this section.

## Part 1: Backend API Configuration for Network Access

TODO: Update this section.

### Step 1.1: Update Backend Configuration

TODO: Update this section.

### Step 1.2: Update CORS Configuration

TODO: Update this section.

### Step 1.3: Update Upload Storage Configuration

TODO: Update this section.

# Update public URL to use your local IP (choose one that's stable)

TODO: Update this document for Sangita Grantha.

### Step 1.4: Start Backend Server

TODO: Update this section.

# Build the project

TODO: Update this document for Sangita Grantha.

# Start the backend API

TODO: Update this document for Sangita Grantha.

## Part 2: Android App Configuration & Deployment

TODO: Update this section.

### Step 2.1: Update API Base URL for Android

TODO: Update this section.

### Step 2.2: Update Network Security Configuration

TODO: Update this section.

### Step 2.3: Add Required Permissions

TODO: Update this section.

### Step 2.4: Build and Install Android App

TODO: Update this section.

# Connect Android device via USB

TODO: Update this document for Sangita Grantha.

# Enable USB debugging on device

TODO: Update this document for Sangita Grantha.

# Accept debugging prompt on device

TODO: Update this document for Sangita Grantha.

# Verify device is connected

TODO: Update this document for Sangita Grantha.

# Build and install debug APK

TODO: Update this document for Sangita Grantha.

# Or run directly

TODO: Update this document for Sangita Grantha.

# Enable WiFi debugging (device must be connected via USB first)

TODO: Update this document for Sangita Grantha.

# Disconnect USB, then connect via WiFi

TODO: Update this document for Sangita Grantha.

# Replace with your Android device's IP

TODO: Update this document for Sangita Grantha.

# Verify connection

TODO: Update this document for Sangita Grantha.

# Install

TODO: Update this document for Sangita Grantha.

# Build release APK (or debug APK)

TODO: Update this document for Sangita Grantha.

# APK will be at

TODO: Update this document for Sangita Grantha.

# androidApp/build/outputs/apk/debug/androidApp-debug.apk

TODO: Update this document for Sangita Grantha.

# Transfer to device via

TODO: Update this document for Sangita Grantha.

# Email attachment

TODO: Update this document for Sangita Grantha.

# Cloud storage (Google Drive, Dropbox)

TODO: Update this document for Sangita Grantha.

# File sharing app (AirDroid, Send Anywhere)

TODO: Update this document for Sangita Grantha.

# USB cable (copy to device storage)

TODO: Update this document for Sangita Grantha.

# Install on device by opening the APK file

TODO: Update this document for Sangita Grantha.

### Step 2.5: Configure App for Testing

TODO: Update this section.

## Part 3: iOS App Configuration & Deployment

TODO: Update this section.

### Step 3.1: Update API Base URL for iOS

TODO: Update this section.

### Step 3.2: Update Info.plist for Local Network Access

TODO: Update this section.

### Step 3.3: Configure Signing & Capabilities

TODO: Update this section.

### Step 3.4: Build and Deploy iOS App

TODO: Update this section.

# Open in Xcode

TODO: Update this document for Sangita Grantha.

# In Xcode

TODO: Update this document for Sangita Grantha.

# 1. Connect iPhone via USB

TODO: Update this document for Sangita Grantha.

# 2. Select your iPhone from the device dropdown

TODO: Update this document for Sangita Grantha.

# 3. Trust the computer on iPhone (prompt will appear)

TODO: Update this document for Sangita Grantha.

# 4. Click Run (▶️) button or Cmd+R

TODO: Update this document for Sangita Grantha.

# 5. On iPhone: Settings > General > Device Management > Trust developer

TODO: Update this document for Sangita Grantha.

# Make sure you're in the iOS app directory

TODO: Update this document for Sangita Grantha.

# Build for device

TODO: Update this document for Sangita Grantha.

# Install (requires connected device)

TODO: Update this document for Sangita Grantha.

# Archive the app

TODO: Update this document for Sangita Grantha.

# Export for TestFlight

TODO: Update this document for Sangita Grantha.

## Part 4: Network Connectivity Verification

TODO: Update this section.

### Step 4.1: Verify Devices are on Same Network

TODO: Update this section.

# Find your network interfaces

TODO: Update this document for Sangita Grantha.

# Should show: 192.168.68.117 or 192.168.68.121

TODO: Update this document for Sangita Grantha.

# Check active connections

TODO: Update this document for Sangita Grantha.

# Via ADB

TODO: Update this document for Sangita Grantha.

# Or in Android device

TODO: Update this document for Sangita Grantha.

# Settings > Network & Internet > WiFi > [Your Network] > Advanced

TODO: Update this document for Sangita Grantha.

# Should show IP in 192.168.68.x range

TODO: Update this document for Sangita Grantha.

### Step 4.2: Test Backend Connectivity from Devices

TODO: Update this section.

### Step 4.3: Test Upload Endpoint

TODO: Update this section.

# From your Mac

TODO: Update this document for Sangita Grantha.

## Part 5: Testing File Upload Functionality

TODO: Update this section.

### Step 5.1: Prepare Test Files

TODO: Update this section.

### Step 5.2: Test Scenarios

TODO: Update this section.

#### Test Case 1: Single Photo Upload

TODO: Update this section.

#### Test Case 2: Multiple File Upload

TODO: Update this section.

#### Test Case 3: Camera Capture & Upload

TODO: Update this section.

#### Test Case 4: Large File Upload

TODO: Update this section.

#### Test Case 5: Network Interruption Handling

TODO: Update this section.

#### Test Case 6: Concurrent Uploads

TODO: Update this section.

### Step 5.3: Verify Upload Results

TODO: Update this section.

# Connect to PostgreSQL

TODO: Update this document for Sangita Grantha.

# Query uploaded documents

TODO: Update this document for Sangita Grantha.

# Get recent uploads (requires authentication)

TODO: Update this document for Sangita Grantha.

## Part 6: Debugging & Troubleshooting

TODO: Update this section.

### Common Issues & Solutions

TODO: Update this section.

#### Issue 1: "Network request failed" on mobile

TODO: Update this section.

#### Issue 2: CORS errors

TODO: Update this section.

#### Issue 3: SSL/TLS certificate errors on iOS

TODO: Update this section.

#### Issue 4: File upload fails with 413 (Payload Too Large)

TODO: Update this section.

#### Issue 5: Uploaded files not accessible

TODO: Update this section.

### Logging & Monitoring

TODO: Update this section.

#### Enable Debug Logging

TODO: Update this section.

# Set log level in application.local.toml or environment

TODO: Update this document for Sangita Grantha.

# View Android logs

TODO: Update this document for Sangita Grantha.

# In Xcode: Debug > Open System Log

TODO: Update this document for Sangita Grantha.

# Or command line

TODO: Update this document for Sangita Grantha.

#### Network Traffic Monitoring

TODO: Update this section.

# Install mitmproxy

TODO: Update this document for Sangita Grantha.

# Run proxy

TODO: Update this document for Sangita Grantha.

# Configure device to use proxy

TODO: Update this document for Sangita Grantha.

# iOS: Settings > WiFi > [Network] > Configure Proxy > Manual

TODO: Update this document for Sangita Grantha.

# Server: 192.168.68.117, Port: 8888

TODO: Update this document for Sangita Grantha.

## Part 7: Performance Testing

TODO: Update this section.

### Upload Speed Testing

TODO: Update this section.

# !/bin/bash

TODO: Update this document for Sangita Grantha.

# test-upload-speed.sh

TODO: Update this document for Sangita Grantha.

### Load Testing

TODO: Update this section.

# Install Apache Bench

TODO: Update this document for Sangita Grantha.

# Test concurrent uploads (requires properly formatted multipart data)

TODO: Update this document for Sangita Grantha.

## Part 8: Production Readiness Checklist

TODO: Update this section.

## Part 9: Quick Reference Commands

TODO: Update this section.

### Start All Services

TODO: Update this section.

# Terminal 1: Start PostgreSQL (if not running)

TODO: Update this document for Sangita Grantha.

# Terminal 2: Start Backend API

TODO: Update this document for Sangita Grantha.

# Terminal 3: Start Admin Web (optional)

TODO: Update this document for Sangita Grantha.

### Deploy to Android

TODO: Update this section.

# Quick deploy

TODO: Update this document for Sangita Grantha.

# Run with logs

TODO: Update this document for Sangita Grantha.

### Deploy to iOS

TODO: Update this section.

# Quick deploy via Xcode

TODO: Update this document for Sangita Grantha.

# Then Cmd+R to run

TODO: Update this document for Sangita Grantha.

### Check Upload Status

TODO: Update this section.

# View recent uploads

TODO: Update this document for Sangita Grantha.

# Check database

TODO: Update this document for Sangita Grantha.

### Network Diagnostics

TODO: Update this section.

# Find your IP

TODO: Update this document for Sangita Grantha.

# Test backend

TODO: Update this document for Sangita Grantha.

# Check open ports

TODO: Update this document for Sangita Grantha.

## Additional Resources

TODO: Update this section.

## Support

TODO: Update this section.
