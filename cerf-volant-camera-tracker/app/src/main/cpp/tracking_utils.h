#ifndef TRACKING_UTILS_H
#define TRACKING_UTILS_H

#ifdef __cplusplus
extern "C" {
#endif

float computeSpeed(float dx, float dy, float dz, float dt);
float computeAzimuth(float dx, float dz);
float emaFilter(float current, float prevFiltered, float alpha);

#ifdef __cplusplus
}
#endif

#endif //TRACKING_UTILS_H
