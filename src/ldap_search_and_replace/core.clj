(ns ldap-search-and-replace.core
  (:require [clj-ldap.client :as ldap]
            [clojure.string :as string]))

(load-file "src/ldap_search_and_replace/credentials.clj") 

(defn -main [& args]

  (def ldap-server (ldap/connect {:host ldap-host
                                  :bind-dn ldap-bind-dn
                                  :password ldap-bind-password
                                  :ssl true}))

  (def record_search_base (nth args 0))
  (def record_attribute (nth args 1))
  (def search_regex (re-pattern (nth args 2)))
  (def replace_string (nth args 3))
  (def search_results (ldap/search ldap-server record_search_base {:size-limit 10000 } ))
  ;(println search_results)

  (doseq [ldap_record_map search_results]
    ;(println ldap_record_map)
    (cond
      ;the syntax to call the anonymous function is ((fn [ARGS] )ARGS) it's two brackets because you have to have a place for the args? 
      (get ldap_record_map (keyword record_attribute)) ((fn []  
        ; in ldap you could have multiple attributes with the same name - a single result gets returned as a string so we have to make sure
        ; everything is a vector.  
        (def attribute_values (conj [] (get ldap_record_map (keyword record_attribute))))
        (doseq [attribute_value (flatten attribute_values)]
        (def uid (get ldap_record_map :dn))
        (cond (re-find search_regex attribute_value) ((fn []
          (println (string/join "\n" ["Processing new record:" attribute_value uid]))
          (def modifiedField (string/replace attribute_value search_regex replace_string))
          (println "modified field:" modifiedField)
          (ldap/modify ldap-server uid { :delete {(keyword record_attribute) attribute_value}})
          (ldap/modify ldap-server uid { :add {(keyword record_attribute) modifiedField}}))))))))))
