(ns com.yetanalytics.dave.ui.app.io)

(defn import-file
  "Read in the text from an input file, resolve the promise, and dispatch."
  [e workbook-id analysis-id key]
  (.preventDefault e)
  (.stopPropagation e)
  (let [target (.. e -currentTarget)]
    ;; Resolve the promise for the text file, should always be the 0th item.
    (.then (js/Promise.resolve
            (.text (aget (.. target -files) 0)))
           #(do 
              (dispatch [:workbook.analysis/update
                         workbook-id
                         analysis-id
                         {key %}])
              ;; clear out the temp file holding the input, so it can be reused.
              (set! (.. target -value) "")))))

(defn export-file
  "Taking in a file blob and a file name, this will create a new anchor element.
   It will use this to download the object."
  [e blob file-name]
  (.preventDefault e)
  (.stopPropagation e)
  (let [link (js/document.createElement "a")]
    (set! (.-download link) file-name)
    ;; webkit does not need the object to be added to the page
    (if js/window.webkitURL
      (set! (.-href link)
            (.createObjectURL js/window.webkitURL
                              blob))
      (do
        ;; for non webkit browsers, add the element to the page
        (set! (.-href link)
              (.createObjectURL js/window.URL
                                blob))
        (set! (.-onclick link)
              (fn [e]
                (.preventDefault e)
                (.stopPropagation e)
                (.remove (.. e -currentTarget))))
        (set! (.. link -style -display)
              "none")
        (.append js/document.body
                 link)))
    (.click link)))
