# Local DNS Setup for Mobile Testing

TODO: Update this document for Sangita Grantha.

## Overview

TODO: Update this section.

## 🌟 If You Have Pi-hole: Prefer Central DNS

TODO: Update this section.

## ⭐ Alternatively: mDNS (Zero Configuration)

TODO: Update this section.

### How it Works

TODO: Update this section.

### Step 1: Find Your Mac's Hostname

TODO: Update this section.

# Check current hostname

TODO: Update this document for Sangita Grantha.

# Example output: Seshadris-MacBook-Pro.local

TODO: Update this document for Sangita Grantha.

# Or: MacBook-Pro.local

TODO: Update this document for Sangita Grantha.

### Step 2: Set a Custom Hostname (Optional)

TODO: Update this section.

# Set computer name

TODO: Update this document for Sangita Grantha.

# Set local hostname

TODO: Update this document for Sangita Grantha.

# Set hostname

TODO: Update this document for Sangita Grantha.

# Verify

TODO: Update this document for Sangita Grantha.

# Should output: -api.local

TODO: Update this document for Sangita Grantha.

### Step 3: Test mDNS Resolution

TODO: Update this section.

# Test from your Mac

TODO: Update this document for Sangita Grantha.

# Should work immediately!

TODO: Update this document for Sangita Grantha.

### Step 4: Update Configuration Files

TODO: Update this section.

#### Backend Configuration (`config/application.local.toml`)

TODO: Update this section.

#### Android Configuration

TODO: Update this section.

#### iOS Configuration

TODO: Update this section.

#### Android Network Security Config

TODO: Update this section.

### Step 5: Test from Mobile Devices

TODO: Update this section.

#### iOS (Works out of the box)

TODO: Update this section.

#### Android (Requires mDNS Support)

TODO: Update this section.

### Advantages of mDNS

TODO: Update this section.

### Limitations

TODO: Update this section.

## Alternative: Local DNS Server (More Complex)

TODO: Update this section.

### Option 1: dnsmasq (Lightweight DNS/DHCP Server)

TODO: Update this section.

#### Install dnsmasq

TODO: Update this section.

#### Configure dnsmasq

TODO: Update this section.

# Edit config

TODO: Update this document for Sangita Grantha.

# Add these lines

TODO: Update this document for Sangita Grantha.

#### Start dnsmasq

TODO: Update this section.

# Start dnsmasq

TODO: Update this document for Sangita Grantha.

# Or run manually

TODO: Update this document for Sangita Grantha.

#### Configure Router

TODO: Update this section.

#### Test

TODO: Update this section.

# From Mac

TODO: Update this document for Sangita Grantha.

# Should return your Mac's IP

TODO: Update this document for Sangita Grantha.

### Option 2: Pi-hole (Most Robust)

TODO: Update this section.

## Comparison Matrix

TODO: Update this section.

## 🎯 Quick Start: mDNS Setup Script

TODO: Update this section.

## Testing Checklist

TODO: Update this section.

### After Setup

TODO: Update this section.

## Troubleshooting

TODO: Update this section.

### mDNS Not Working on Android

TODO: Update this section.

### mDNS Not Working on iOS

TODO: Update this section.

### Can't Ping Hostname

TODO: Update this section.

## Best Practices

TODO: Update this section.

### For Development

TODO: Update this section.

### For Production

TODO: Update this section.

### Configuration Management

TODO: Update this section.

## Summary

TODO: Update this section.

### Recommended Approach: mDNS

TODO: Update this section.

### Why This Works

TODO: Update this section.

## Next Steps

TODO: Update this section.
