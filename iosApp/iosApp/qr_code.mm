/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#import "qr_code.h"

#import <AVFoundation/AVFoundation.h>
#import <UIKit/UIKit.h>

#import <atomic>

#import "qrcode/ios/device_params_helper.h"
#import "qrcode/ios/qr_scan_view_controller.h"
#import "util/logging.h"

namespace cardboard {
namespace qrcode {
namespace {

std::atomic<int32_t> deviceParamsChangedCount = {0};

void incrementDeviceParamsChangedCount() { std::atomic_fetch_add(&deviceParamsChangedCount, 1); }

UIViewController *getPresentingViewController() {
    UIViewController *presentingViewController = nil;
    presentingViewController = [UIApplication sharedApplication].keyWindow.rootViewController;
    while (presentingViewController.presentedViewController) {
      presentingViewController = presentingViewController.presentedViewController;
    }
    if (presentingViewController.isBeingDismissed) {
      presentingViewController = presentingViewController.presentingViewController;
    }
    return presentingViewController;
}

void showQRScanViewController() {
  UIViewController *presentingViewController = getPresentingViewController();

  __block CardboardQRScanViewController *qrViewController =
      [[CardboardQRScanViewController alloc] initWithCompletion:^(BOOL /*succeeded*/) {
        incrementDeviceParamsChangedCount();
        [qrViewController dismissViewControllerAnimated:YES completion:nil];
      }];

  qrViewController.modalTransitionStyle = UIModalTransitionStyleCrossDissolve;
  [presentingViewController presentViewController:qrViewController animated:YES completion:nil];
}

void requestPermissionInSettings() {
    UIViewController *presentingViewController = getPresentingViewController();

    // Show an alert that the camera is required
    UIAlertController *alert = [UIAlertController
                                alertControllerWithTitle:@"Camera Required"
                                message:@"The camera is required to scan QR codes to configure for your headset. Press 'Enable in Settings' to enable the camera for this app. Press 'Use Defaults' to use the default headset configuration which may or may not work correctly."
                                preferredStyle:UIAlertControllerStyleAlert];
    UIAlertAction *enable = [UIAlertAction
                             actionWithTitle:@"Enable in Settings"
                             style:UIAlertActionStyleDefault
                             handler:^(UIAlertAction * action) {
        // Open settings
        NSURL *settingURL = [NSURL URLWithString:UIApplicationOpenSettingsURLString];
        [UIApplication.sharedApplication openURL:settingURL options:@{} completionHandler:nil];
    }];
    [alert addAction:enable];
    UIAlertAction *cancel = [UIAlertAction
                             actionWithTitle:@"Use Defaults"
                             style:UIAlertActionStyleCancel
                             handler:^(UIAlertAction * action) {
        // Set defaults
        NSData *deviceParams = [CardboardDeviceParamsHelper readSerializedDeviceParams];
        if (deviceParams.length == 0) {
          [CardboardDeviceParamsHelper saveCardboardV1Params];
        }
        incrementDeviceParamsChangedCount();
    }];
    [alert addAction:cancel];
    [presentingViewController presentViewController:alert animated:YES completion:nil];
}

void prerequestCameraPermissionForQRScan() {
  [AVCaptureDevice requestAccessForMediaType:AVMediaTypeVideo
                           completionHandler:^(BOOL granted) {
                             dispatch_async(dispatch_get_main_queue(), ^{
                               if (granted) {
                                 showQRScanViewController();
                               } else {
                                 requestPermissionInSettings();
                               }
                             });
                           }];
}

}  // anonymous namespace

std::vector<uint8_t> getCurrentSavedDeviceParams() {
  NSData *deviceParams = [CardboardDeviceParamsHelper readSerializedDeviceParams];
  std::vector<uint8_t> result(
      static_cast<const uint8_t *>(deviceParams.bytes),
      static_cast<const uint8_t *>(deviceParams.bytes) + deviceParams.length);
  return result;
}

void scanQrCodeAndSaveDeviceParams() {
  switch ([AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo]) {
    case AVAuthorizationStatusAuthorized:
      // We already have camera permissions - proceed to QR scan controller.
      showQRScanViewController();
      return;

    case AVAuthorizationStatusNotDetermined:
      // We have not yet shown the camera request prompt.
      prerequestCameraPermissionForQRScan();
      return;

    default:
      // We've had permission explicitly rejected before.
      requestPermissionInSettings();
      return;
  }
}

void saveDeviceParams(const uint8_t *uri, int /*size*/) {
  NSString *uriAsString = [NSString stringWithUTF8String:reinterpret_cast<const char *>(uri)];
  if (![uriAsString hasPrefix:@"http://"] && ![uriAsString hasPrefix:@"https://"]) {
    uriAsString = [NSString stringWithFormat:@"%@%@", @"https://", uriAsString];
  }

  // Check whether the URI is valid.
  NSURL *url = [NSURL URLWithString:uriAsString];
  if (!url) {
    CARDBOARD_LOGE("Invalid URI: %@", uriAsString);
    return;
  }

  // Get the device params from the provided URL and save them to storage.
  [CardboardDeviceParamsHelper
      resolveAndUpdateViewerProfileFromURL:url
                            withCompletion:^(BOOL success, NSError *error) {
                              if (success) {
                                CARDBOARD_LOGI("Successfully saved device parameters to storage");
                              } else {
                                if (error) {
                                  CARDBOARD_LOGE(
                                      "Error when trying to get the device params from the URI: %@",
                                      error);
                                } else {
                                  CARDBOARD_LOGE("Error when saving device parameters to storage");
                                }
                              }
                            }];
}

int getDeviceParamsChangedCount() { return deviceParamsChangedCount; }

}  // namespace qrcode
}  // namespace cardboard
