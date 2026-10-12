import { useState } from 'react'
import { getPosterSources } from '../utils/posters.js'

export default function PosterImage({ movie, ...imageProps }) {
  const sources = getPosterSources(movie)
  const sourcesKey = JSON.stringify(sources)
  const [failedSource, setFailedSource] = useState({ key: '', index: 0 })
  const sourceIndex = failedSource.key === sourcesKey ? failedSource.index : 0

  const handleError = () => {
    if (sourceIndex < sources.length - 1) {
      setFailedSource({ key: sourcesKey, index: sourceIndex + 1 })
    }
  }

  return <img {...imageProps} src={sources[sourceIndex]} onError={handleError} />
}
