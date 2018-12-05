(TeX-add-style-hook
 "algorithm_template"
 (lambda ()
   (TeX-add-to-alist 'LaTeX-provided-package-options
                     '(("algorithm2e" "ruled")))
   (add-to-list 'LaTeX-verbatim-environments-local "lstlisting")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "path")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "url")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "nolinkurl")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "hyperbaseurl")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "hyperimage")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "hyperref")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "lstinline")
   (add-to-list 'LaTeX-verbatim-macros-with-delims-local "path")
   (add-to-list 'LaTeX-verbatim-macros-with-delims-local "lstinline")
   (TeX-run-style-hooks
    "latex2e"
    "article"
    "art10"
    "algorithm2e"
    "hyperref"
    "amsmath"
    "zed-csp"
    "breqn"
    "listings")
   (LaTeX-add-labels
    "moreLink")
   (LaTeX-add-environments
    '("info" LaTeX-env-args ["argument"] 0)
    '("warn" LaTeX-env-args ["argument"] 0)
    '("question" LaTeX-env-args ["argument"] 0)
    '("file" LaTeX-env-args ["argument"] 0)))
 :latex)

