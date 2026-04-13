import { registerPlugin } from '@capacitor/core';

import type { TripTrackerPlugin } from './definitions';

const TripTracker = registerPlugin<TripTrackerPlugin>('TripTracker');

export * from './definitions';
export { TripTracker };
