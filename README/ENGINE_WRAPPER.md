# Engine Wrapper

The wrapper isolates Orca internals behind a stable API.

## API

```c
typedef struct OrcaEngine OrcaEngine;

OrcaEngine* orca_create();
void orca_destroy(OrcaEngine* engine);

int orca_load_model(OrcaEngine* engine, const char* path);
int orca_set_config_json(OrcaEngine* engine, const char* json);
int orca_slice(OrcaEngine* engine);

const char* orca_get_gcode(OrcaEngine* engine);
```

## Rules

* No C++ types exposed
* No exceptions across boundary
* JSON for structured data
* Wrapper absorbs upstream changes

## Current State

* Desktop wrapper integration exists for headless Orca validation
* The reduced Android Orca core still exists as a narrow compatibility layer
* Android JNI must include `orca_wrapper.h` only
* Android model loading and slicing/export stay behind the wrapper contract
* The real Orca-style slicing/export path is proven through the integrated
  shipping ARM app path for the currently accepted fixture/profile boundary
* The current wrapper-level open boundary is broader fixture/profile coverage,
  not first proof of shipping-stack runtime integration
