#!/bin/bash
# Mirror all branches from github to origin
for branch in $(git branch -r | grep '^\s*github/' | sed 's/.*github\///'); do
  echo "Pushing github/$branch -> origin/$branch"
  git push origin "github/$branch:refs/heads/$branch"
done

# Mirror all tags
echo "Pushing all tags..."
git push origin --tags
