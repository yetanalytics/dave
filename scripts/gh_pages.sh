#!/bin/bash

# clean everything up
rm -rf target node_modules package* gh_pages

# make the dir
mkdir -p gh_pages/cljs-out

# copy in the pub dir
cp -rv resources/public/* gh_pages/

# remove the dev/test index, and any css/maps
rm gh_pages/index.html gh_pages/test.html gh_pages/css/style.css*

# make the prod index the only one
mv gh_pages/prod_index.html gh_pages/index.html

# build the js
clojure -A:build-prod

# move it in to the GH pages package
cp target/public/cljs-out/prod-main.js gh_pages/cljs-out/

# build the css (node_modules must be there for mdc)
clojure -A:build-sass

# move it
cp resources/public/css/style.css gh_pages/css/

# Done!
exit 0
