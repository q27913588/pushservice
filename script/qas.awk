/^[[:blank:]]*qas:/, /^[[:blank:]]*reg:/ {
    if ($1 == "PUSHSERVICE_VERSION:")
        print "      " $1 " " new_ver
    else
        print
    next
}
1