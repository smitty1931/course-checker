#!/bin/bash
sem=$1
courses=$(cat tocheck.txt)

mkdir search
cd search

echo $courses

codes=$(echo "$courses" | tr ' ' '\n' | cut -c 1-4 | sort | uniq)

for code in $codes; do
    command="https://api.easi.utoronto.ca/ttb/getOptimizedMatchingCourseTitles?term=$code&divisions=ARTSC&sessions=$sem&lowerThreshold=50&upperThreshold=200"
    curl --location "$command" > "$code.xml"
done

for course in $courses; do
    code=$(echo $course | cut -c 1-4)
    title=$(echo $course | cut -c 5-)
    grep -q "$title" "$code.xml"
    if [ $? -eq 0 ]; then
        echo "Found $course"
    fi
done

cd ..
rm -rf search