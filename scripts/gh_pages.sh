#!/bin/bash

# TODO: SASS

# clean everything up
rm -rf target node_modules package* gh_pages

# make the dir
mkdir -p gh_pages/cljs-out

# copy in the pub dir
cp -rv resources/public/* gh_pages/

# remove the dev index and replace it with prod
rm gh_pages/index.html gh_pages/test.html
mv gh_pages/prod_index.html gh_pages/index.html

# build the js
clojure -A:build-prod

# move it in to the GH pages package
cp target/public/cljs-out/prod-main.js gh_pages/cljs-out/

# Done!
exit 0
