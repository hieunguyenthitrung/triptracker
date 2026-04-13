require 'json'
package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = 'CapacitorTripTracker'
  s.version      = package['version']
  s.summary      = package['description']
  s.license      = 'MIT'
  s.homepage     = 'https://github.com/hieunguyentt/TripTracker'
  s.author       = 'CarMD'
  s.source       = { :git => 'https://github.com/hieunguyentt/TripTracker.git', :tag => s.version.to_s }
  s.source_files = 'ios/Plugin/**/*.{swift,h,m,c,cc,mm,cpp}'
  s.ios.deployment_target = '14.0'
  s.dependency 'Capacitor'
  s.swift_version = '5.9'
  s.frameworks = 'UIKit', 'CoreLocation', 'CoreMotion', 'MapKit',
                 'AVFoundation', 'UserNotifications', 'Network'
  s.libraries = 'sqlite3'
  s.weak_frameworks = 'CarPlay'
end
