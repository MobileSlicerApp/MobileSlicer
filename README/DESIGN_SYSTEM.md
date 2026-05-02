# Design System

## Principles

* Clean
* Minimal
* Card-based
* Touch-first

## Layout

* Hero card (Import)
* Profiles card on the home grid
* Single top-right app-bar Settings action
* Section blocks (Recent plates)

## Colors

* Dark theme primary
* Soft gradients
* Accent palette system

## Components

Buttons:

* Primary (filled)
* Secondary (outlined)

Cards:

* Rounded
* Elevated or gradient

## Interaction

* Bottom sheets
* Tabs
* Expandable sections

## Rule

All new UI must match this system.

## Standing Product Rule

Use OrcaSlicer as the source of truth for:

* profile categories
* settings structure
* slicer terminology
* preset concepts

Use committed design inspiration files and product logo assets as the source of
truth for:

* visual aesthetic
* card treatment
* page composition
* top-bar branding
* mobile interaction feel

Current repo hygiene note: only committed files should be treated as available
references. The previously tracked screenshot references were intentionally
deleted.

Do:

* keep the UI touch-first
* adapt Orca concepts into curated Android flows
* use the product logo in the top-left app shell consistently
* use a cog-style icon for the single app-settings entry point in the top-right area
* keep slicer-configuration actions under `Profiles`

Do not:

* recreate Orca desktop screens directly
* dump raw desktop settings panels into the app
* let product UI drift away from the inspiration aesthetic
* duplicate app settings as both a home card and a top-bar action
