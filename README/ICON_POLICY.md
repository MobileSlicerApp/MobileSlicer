# Icon Policy

Generic Compose UI actions should use Compose Material icons.

Use Compose Material icons for common UI actions such as back, settings, delete,
clone, add, share, export, save, refresh, menu, close, and search.

Keep drawable resources for:

* brand artwork, including `ic_logo.xml`
* launcher/system metadata assets
* custom product graphics that do not map to a standard Material icon
* assets that require exact brand geometry, gradients, or Android resource
  integration outside Compose

Do not add a new drawable XML for a generic action icon unless there is a
specific product reason and the reason is documented near the call site.
