/^[[:blank:]]*reg:/, /^[[:blank:]]*prod:/ {
    if ($1 == "PUSHSERVICE_VERSION:")
        print "      " $1 " " new_ver
    else
        print
    next
}
1