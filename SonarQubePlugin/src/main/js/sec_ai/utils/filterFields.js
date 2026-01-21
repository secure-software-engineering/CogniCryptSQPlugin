export default function filterCpgField(data) {
  // Return original data if it's not a valid object
  if (!data || typeof data !== 'object') {
    return data;
  }

  // If the data is an array, map over it and filter each object
  if (Array.isArray(data)) {
    return data.map(item => {
      const { cpgBase64Gz, ...rest } = item;
      return rest;
    });
  }

  // If the data is a single object, filter it
  const { cpgBase64Gz, ...rest } = data;
  return rest;
}

