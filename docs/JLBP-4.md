# [JLBP-4] Avoid dependencies on unstable libraries and features

Do not depend on libraries that are pre-1.0 or on unstable features in
libraries that are 1.0 or later.
Unstable libraries allow breaking changes to their
public APIs within the same major version. For libraries following semantic
versioning, this means libraries with a 0.x.y version. See [JLBP-3](JLBP-3.md)
for more details on semantic versioning.
Unstable features within an otherwise stable library
are marked with an annotation such as `@Beta`. See
[JLBP-3](JLBP-3.md) for more details on annotating unstable features.

If your library depends on an unstable library or feature which
experiences a breaking change between versions, your library is locked to
a specific version of that dependency.
If you expose the unstable feature on your library's surface, then your
library's current major version is permanently locked to the version
you initially exposed, and you cannot upgrade without making a
breaking change to your users.
If you only use the unstable feature in your implementation, then each minor
or patch version of your library requires a very specific version of
your dependency. It is unsafe for users to upgrade your library on its own,
creating opportunities for hard-to-diagnose runtime conflicts.

Depending on unstable features between submodules of a single library is
acceptable, provided that users can easily force their build system to use
compatible versions of the submodules. Some strategies for library owners
include:

- Using the same version number for all submodules in a library
- Providing a [BOM](http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Importing_Dependencies)
