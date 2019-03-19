# kup-platform

KAISquare Unified Platform (juzz4)

## Packaging

Run the gradle task
```
./gradlew packJuzz4 \
-Dprefix=platform \
-Dtype=<cloud|node> \
-Dversion=x.x.x.x \
-Dcloud_host=<kup-site>
```
Output package
```
./build-ks/<prefix>-<type>-<version>.tgz
```

See examples below for reference
```
./sample_pack_cloud.sh
./sample_pack_node.sh
```

**Notes**

* On windows, `packJuzz4` task must be run inside `git-bash.exe` because the task uses `tar`


## juzz4 dependencies
```
## Print dependency tree
$ gradle -q dependencies --configuration compile
## Check for newer dependency updates using gradle-versions-plugin
$ gradle -q dependencyUpdates -Drevision=release
```